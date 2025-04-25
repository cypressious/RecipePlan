@file:Suppress("UsePropertyAccessSyntax")

package de.rakhman.cooking.states

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import de.rakhman.cooking.Database
import de.rakhman.cooking.PlatformContext
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.events.*
import de.rakhman.cooking.updateWidget
import io.sellmair.evas.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

sealed class RecipesState : State {
    data object Loading : RecipesState()
    data class Success(val recipes: List<Recipe>, val plan: List<Long>, val shop: List<Long>) : RecipesState() {
        val byId = recipes.associateBy { it.id }
    }

    companion object Key : State.Key<RecipesState> {
        override val default: RecipesState get() = Loading
    }
}

private const val SHEET_NAME_RECIPES = "Rezepte"
private const val SHEET_NAME_PLAN = "Plan"
private const val RANGE_PLAN_AND_SHOP = "$SHEET_NAME_PLAN!A1:A2"
private const val DELETED_VALUE = "deleted"

private const val COLUMN_NAME = 0
private const val COLUMN_URL = 1
private const val COLUMN_DELETED = 2
private const val COLUMN_COUNTER = 3

const val ID_TEMPORARY = -1L

class RecipeContext(
    val database: Database,
    val sheets: Sheets,
    val spreadSheetsId: String,
    val platformContext: PlatformContext,
    val repositoryFactory: de.rakhman.cooking.repository.RepositoryFactory = de.rakhman.cooking.repository.RepositoryFactory(
        database,
        sheets,
        spreadSheetsId,
        platformContext
    )
)

fun CoroutineScope.launchRecipesState(
    database: Database,
    sheets: Deferred<Sheets>,
    platformContext: PlatformContext
) {
    launchState(SettingsState) {
        val spreadSheetsId = database.settingsQueries.selectFirst().executeAsOneOrNull()
        if (spreadSheetsId != null) {
            emit(SettingsState(spreadSheetsId))
        } else {
            ScreenState.set(ScreenState.Settings)
        }

        collectEventsAsync<SpreadsheetIdChangedEvent> {
            val id = it.id

            database.transaction {
                database.settingsQueries.deleteAll()
                id?.let { database.settingsQueries.insert(it) }
                database.recipesQueries.deleteAll()
                database.planQueries.deleteAll()
                database.shopQueries.deleteAll()
            }

            emit(id?.let(::SettingsState))
        }

        collectEventsAsync<CreateSpreadsheetEvent>(Dispatchers.IO) {
            try {
                SavingSettingsState.set(SavingSettingsState.Saving)
                val sheets = sheets.await()
                val result = sheets.spreadsheets().create(Spreadsheet().apply {
                    properties = SpreadsheetProperties().apply { title = "Recipes" }
                    this.sheets = listOf(
                        Sheet().apply { properties = SheetProperties().apply { title = SHEET_NAME_RECIPES } },
                        Sheet().apply { properties = SheetProperties().apply { title = SHEET_NAME_PLAN } },
                    )
                }).execute()
                SpreadsheetIdChangedEvent(result.spreadsheetId).emit()
            } catch (e: Exception) {
                ErrorEvent(e).emit()
            } finally {
                SavingSettingsState.set(SavingSettingsState.NotSaving)
            }
        }
    }

    launch {
        SettingsState.flow().collectLatest {
            val spreadSheetsId = it?.spreadSheetsId
            if (!spreadSheetsId.isNullOrBlank()) {
                coroutineScope {
                    with(RecipeContext(database, sheets.await(), spreadSheetsId, platformContext)) {
                        launchRecipesStateInternal()
                    }
                }
            }
        }
    }
}

context(c: RecipeContext)
private fun CoroutineScope.launchRecipesStateInternal() = launch {
    setStateFromDatabase(c.database) // The database parameter is not used in the method, but kept for compatibility
    collectEventsAsyncCatchingErrors<ReloadEvent> { syncWithSheets() }
    collectEventsAsyncCatchingErrors<DeleteEvent> { setDeleted(it.id) }
    collectEventsAsyncCatchingErrors<AddEvent> { addRecipe(it.title, it.url, it.target) }
    collectEventsAsyncCatchingErrors<UpdateEvent> { updateRecipe(it.id, it.title, it.url) }
    collectEventsAsyncCatchingErrors<AddToPlanEvent> {
        updatePlanAndShop(addIdToPlan = it.id, removeIdFromShop = it.id)
    }
    collectEventsAsyncCatchingErrors<RemoveFromPlanEvent> {
        updatePlanAndShop(removeIdFromPlan = it.id, incrementCounter = true)
    }
    collectEventsAsyncCatchingErrors<AddToShopEvent> {
        updatePlanAndShop(addIdToShop = it.id)
    }
    collectEventsAsyncCatchingErrors<RemoveFromShopEvent> {
        updatePlanAndShop(removeIdFromShop = it.id)
    }

    ReloadEvent.emit()
}

context(c: RecipeContext)
private inline fun <reified T : Event> CoroutineScope.collectEventsAsyncCatchingErrors(
    crossinline f: suspend (T) -> Unit
) {
    collectEventsAsync<T> { event ->
        try {
            SyncState.setSyncing()
            f(event)
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorEvent(e).emit()
            setStateFromDatabase(c.database)
        } finally {
            SyncState.setNotSyncing()
        }
    }
}

context(c: RecipeContext)
private suspend fun setDeleted(id: Long) {
    val state = RecipesState.value()
    if (state is RecipesState.Success) {
        RecipesState.set(RecipesState.Success(state.recipes.filter { it.id != id }, state.plan, state.shop))
    }

    // Use the recipe repository to delete the recipe
    c.repositoryFactory.recipeRepository.deleteRecipe(id)

    // Update the UI with the latest data
    syncWithSheets()
}

context(c: RecipeContext)
private fun updateRawValue(range: String, rawValues: List<List<String>>) {
    c.sheets.spreadsheets().values().update(
        c.spreadSheetsId,
        range,
        ValueRange().apply { setValues(rawValues) }
    ).run {
        valueInputOption = "RAW"
        execute()
    }
}

context(c: RecipeContext)
private suspend fun updatePlanAndShop(
    addIdToPlan: Long? = null,
    addIdToShop: Long? = null,
    removeIdFromPlan: Long? = null,
    removeIdFromShop: Long? = null,
    updateStateOptimistically: Boolean = true,
    incrementCounter: Boolean = false,
) {
    fun newPlanAndShop(
        plan: List<Long>,
        shop: List<Long>,
    ): Pair<List<Long>, List<Long>> {
        fun List<Long>.updatePlan(addId: Long?, removeId: Long?): List<Long> {
            return buildList {
                this@updatePlan.filterTo(this) { id -> id != removeId && id != ID_TEMPORARY }
                addId?.let { add(it) }
            }.distinct()
        }

        val newPlan = plan.updatePlan(addIdToPlan, removeIdFromPlan)
        val newShop = shop.updatePlan(addIdToShop, removeIdFromShop)
        return Pair(newPlan, newShop)
    }

    if (updateStateOptimistically) {
        val state = RecipesState.value()
        if (state is RecipesState.Success) {
            val (newPlan, newShop) = newPlanAndShop(state.plan, state.shop)
            RecipesState.set(RecipesState.Success(state.recipes, newPlan, newShop))
        }
    }

    withContext(Dispatchers.IO) {
        // Update plan
        addIdToPlan?.let { c.repositoryFactory.planRepository.addToPlan(it) }
        removeIdFromPlan?.let { c.repositoryFactory.planRepository.removeFromPlan(it) }

        // Update shop
        addIdToShop?.let { c.repositoryFactory.shopRepository.addToShop(it) }
        removeIdFromShop?.let { c.repositoryFactory.shopRepository.removeFromShop(it) }

        // Increment counter if needed
        if (incrementCounter && removeIdFromPlan != null) {
            c.repositoryFactory.recipeRepository.incrementCounter(removeIdFromPlan)
        }
    }

    // Update the UI with the latest data
    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun updateRecipe(id: Long, title: String, url: String?) {
    val state = RecipesState.value()
    val target = (ScreenState.value() as? ScreenState.Add)?.target
    ScreenState.set(target ?: ScreenState.Recipes)

    if (state is RecipesState.Success) {
        RecipesState.set(
            RecipesState.Success(
                state.recipes.map { if (it.id == id) Recipe(id, title, url, it.counter) else it },
                state.plan,
                state.shop,
            )
        )
    }

    // Use the recipe repository to update the recipe
    c.repositoryFactory.recipeRepository.updateRecipe(id, title, url)

    // No need to call syncWithSheets() as the repository handles synchronization
}

context(c: RecipeContext)
private suspend fun addRecipe(title: String, url: String?, target: ScreenState.BaseScreen) {
    ScreenState.set(target)

    val state = RecipesState.value()
    if (state is RecipesState.Success) {
        RecipesState.set(
            RecipesState.Success(
                state.recipes + Recipe(ID_TEMPORARY, title, url, 0),
                state.plan.let { if (target == ScreenState.Plan) it + ID_TEMPORARY else it },
                state.shop.let { if (target == ScreenState.Shop) it + ID_TEMPORARY else it },
            )
        )
    }

    // Use the recipe repository to add the recipe
    val id = c.repositoryFactory.recipeRepository.addRecipe(title, url)

    // Add to plan or shop if needed
    if (target == ScreenState.Shop) {
        c.repositoryFactory.shopRepository.addToShop(id)
    } else if (target == ScreenState.Plan) {
        c.repositoryFactory.planRepository.addToPlan(id)
    }

    // Update the UI with the latest data
    syncWithSheets()
}

context(c: RecipeContext)
suspend fun syncWithSheets() = withContext(Dispatchers.IO) {
    // Use the repository factory to sync all data
    c.repositoryFactory.syncAll()

    // Get the data from repositories
    val recipes = c.repositoryFactory.recipeRepository.getAllRecipes()
    val plan = c.repositoryFactory.planRepository.getPlanRecipeIds()
    val shop = c.repositoryFactory.shopRepository.getShopRecipeIds()

    withContext(Dispatchers.Default) { updateWidget(c.platformContext) }

    coroutineContext.statesOrNull?.setState(RecipesState, RecipesState.Success(recipes, plan, shop))
}

context(c: RecipeContext)
private suspend fun setStateFromDatabase(database: Database) {
    // Use the repositories to get the data
    val recipes = c.repositoryFactory.recipeRepository.getAllRecipes()
    val plan = c.repositoryFactory.planRepository.getPlanRecipeIds()
    val shop = c.repositoryFactory.shopRepository.getShopRecipeIds()

    RecipesState.set(
        if (recipes.isNotEmpty()) {
            RecipesState.Success(
                recipes = recipes,
                plan = plan,
                shop = shop,
            )
        } else {
            RecipesState.Loading
        }
    )
}

// The following methods have been replaced by the repository pattern implementation
