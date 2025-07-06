package de.rakhman.cooking.states

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import de.rakhman.cooking.Database
import de.rakhman.cooking.PlatformContext
import de.rakhman.cooking.events.*
import de.rakhman.cooking.repositories.DatabaseRepository
import de.rakhman.cooking.repositories.SHEET_NAME_PLAN
import de.rakhman.cooking.repositories.SHEET_NAME_RECIPES
import de.rakhman.cooking.repositories.SheetsRepository
import de.rakhman.cooking.updateWidget
import io.sellmair.evas.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

sealed class RecipesState : State {
    data object Loading : RecipesState()
    data class Success(val recipes: List<RecipeDto>, val plan: List<Long>, val shop: List<Long>) : RecipesState() {
        val byId: Map<Long, RecipeDto> = recipes.associateBy { it.id }
        val allTags: Set<String> = recipes.flatMap { it.tags }.sorted().toSet()
    }

    companion object Key : State.Key<RecipesState> {
        override val default: RecipesState get() = Loading
    }
}

class RecipeDto private constructor(
    val id: Long,
    val title: String,
    val url: String?,
    val counter: Long,
    val tags: Set<String>,
) {
    init {
        require(url == null || !url.isEmpty()) { "URL must be null or non-empty" }
        require(tags.none { it.isBlank() }) { "Tags must not contain blank items" }
    }

    companion object {
        const val SEPARATOR_TAGS = ";"

        fun create(
            id: Long,
            title: String,
            url: String?,
            counter: Long,
            tagsString: String?,
        ) = create(
            id = id,
            title = title,
            url = url,
            counter = counter,
            tagsSet = tagsString?.trim()?.split(SEPARATOR_TAGS)?.mapTo(mutableSetOf()) { it.trim() }?.filter { it.isNotBlank() }?.toSet().orEmpty()
        )

        fun create(
            id: Long,
            title: String,
            url: String?,
            counter: Long,
            tagsSet: Set<String>,
        ) = RecipeDto(id, title, url?.ifBlank { null }, counter, tagsSet)
    }
}

const val ID_TEMPORARY = -1L

class RecipeContext(
    val database: DatabaseRepository,
    val sheets: SheetsRepository,
    val platformContext: PlatformContext
)

fun CoroutineScope.launchRecipesState(
    database: Database,
    sheets: Deferred<Sheets>,
    platformContext: PlatformContext
) {
    val databaseRepository = DatabaseRepository(database)
    launchState(SettingsState) {
        val spreadSheetsId = databaseRepository.getSpreadSheetIdOrNull()
        if (spreadSheetsId != null) {
            emit(SettingsState(spreadSheetsId))
        } else {
            ScreenState.set(ScreenState.Settings)
        }

        collectEventsAsync<SpreadsheetIdChangedEvent> {
            val id = it.id

            databaseRepository.updateSpreadsheetId(id)

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
                    with(RecipeContext(
                        database = databaseRepository,
                        sheets = SheetsRepository(sheets.await(), spreadSheetsId),
                        platformContext = platformContext
                    )) {
                        launchRecipesStateInternal()
                    }
                }
            }
        }
    }
}

context(c: RecipeContext)
private fun CoroutineScope.launchRecipesStateInternal() = launch {
    setStateFromDatabase(c.database)
    collectEventsAsyncCatchingErrors<ReloadEvent> { syncWithSheets() }
    collectEventsAsyncCatchingErrors<DeleteEvent> { setDeleted(it.id) }
    collectEventsAsyncCatchingErrors<AddEvent> { addRecipe(it.title, it.url, it.target, it.tags) }
    collectEventsAsyncCatchingErrors<UpdateEvent> { updateRecipe(it.id, it.title, it.url, it.tags) }
    collectEventsAsyncCatchingErrors<AddToPlanEvent> {
        updatePlanAndShop(addIdToPlan = it.id, removeIdFromShop = it.id)
    }
    collectEventsAsyncCatchingErrors<RemoveFromPlanEvent> {
        updatePlanAndShop(removeIdFromPlan = it.id, incrementCounter = it.incrementCounter)
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
    withContext(Dispatchers.IO) {
        c.sheets.delete(id)
    }
    syncWithSheets()
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
        val (oldPlan, oldShop) = c.sheets.getPlanAndShop()
        val (newPlan, newShop) = newPlanAndShop(oldPlan, oldShop)

        c.sheets.updatePlanAndShop(
            newPlan = newPlan,
            newShop = newShop,
            idToIncrementCounter = removeIdFromPlan.takeIf { incrementCounter })
    }
    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun updateRecipe(id: Long, title: String, url: String?, tags: Set<String>) {
    val state = RecipesState.value()
    val target = (ScreenState.value() as? ScreenState.Add)?.target
    ScreenState.set(target ?: ScreenState.Recipes)

    if (state is RecipesState.Success) {
        RecipesState.set(
            RecipesState.Success(
                state.recipes.map { if (it.id == id) RecipeDto.create(id, title, url, it.counter, tags) else it },
                state.plan,
                state.shop,
            )
        )
    }

    withContext(Dispatchers.IO) {
        c.sheets.updateRecipe(id, title, url, tags)
    }

    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun addRecipe(title: String, url: String?, target: ScreenState.BaseScreen, tags: Set<String>) {
    ScreenState.set(target)

    val state = RecipesState.value()
    if (state is RecipesState.Success) {
        RecipesState.set(
            RecipesState.Success(
                state.recipes + RecipeDto.create(ID_TEMPORARY, title, url, 0, tags),
                state.plan.let { if (target == ScreenState.Plan) it + ID_TEMPORARY else it },
                state.shop.let { if (target == ScreenState.Shop) it + ID_TEMPORARY else it },
            )
        )
    }

    withContext(Dispatchers.IO) {
        val id = c.sheets.getNewId()
        c.sheets.updateRecipe(id, title, url, tags)

        if (target == ScreenState.Shop) {
            updatePlanAndShop(
                addIdToShop = id,
                updateStateOptimistically = false
            )
        } else if (target == ScreenState.Plan) {
            updatePlanAndShop(
                addIdToPlan = id,
                removeIdFromShop = null,
                updateStateOptimistically = false
            )
        }
    }

    syncWithSheets()
}

context(c: RecipeContext)
suspend fun syncWithSheets() = withContext(Dispatchers.IO) {
    val recipesDeferred = async { c.sheets.getRecipes() }
    val planDeferred = async { c.sheets.getPlanAndShop() }

    val recipes = recipesDeferred.await()
    val (plan, shop) = planDeferred.await()

    c.database.updateWith(recipes, plan, shop)
    withContext(Dispatchers.Default) { updateWidget(c.platformContext) }

    coroutineContext.statesOrNull?.setState(RecipesState, RecipesState.Success(recipes, plan, shop))
}

private suspend fun setStateFromDatabase(database: DatabaseRepository) {
    val recipes = database.getRecipes()
    RecipesState.set(
        if (recipes.isNotEmpty()) {
            RecipesState.Success(
                recipes = recipes,
                plan = database.getPlan(),
                shop = database.getShop(),
            )
        } else {
            RecipesState.Loading
        }
    )
}
