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

const val SEPARATOR_TAGS = ";"

sealed class RecipesState : State {
    data object Loading : RecipesState()
    data class Success(val recipes: List<Recipe>, val plan: List<Long>, val shop: List<Long>) : RecipesState() {
        val byId: Map<Long, Recipe> = recipes.associateBy { it.id }
        val allTags: Set<String> = recipes.flatMap { it.tagsSet }.sorted().toSet()
    }

    companion object Key : State.Key<RecipesState> {
        override val default: RecipesState get() = Loading
    }
}

val Recipe.tagsSet: Set<String>
    get() = tags?.split(SEPARATOR_TAGS)?.mapTo(mutableSetOf()) { it.trim() }.orEmpty()

private const val SHEET_NAME_RECIPES = "Rezepte"
private const val SHEET_NAME_PLAN = "Plan"
private const val RANGE_PLAN_AND_SHOP = "$SHEET_NAME_PLAN!A1:A2"
private const val DELETED_VALUE = "deleted"

private const val COLUMN_NAME = 0
private const val COLUMN_URL = 1
private const val COLUMN_DELETED = 2
private const val COLUMN_COUNTER = 3
private const val COLUMN_TAGS = 4

const val ID_TEMPORARY = -1L

class RecipeContext(
    val database: Database,
    val sheets: Sheets,
    val spreadSheetsId: String,
    val platformContext: PlatformContext
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
        updateRawValue("$SHEET_NAME_RECIPES!C${id}", listOf(listOf(DELETED_VALUE)))
    }
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
        awaitAll(
            async {
                val (oldPlan, oldShop) = readPlanAndShop()
                val (newPlan, newShop) = newPlanAndShop(oldPlan, oldShop)

                updateRawValue(
                    RANGE_PLAN_AND_SHOP,
                    listOf(
                        listOf(newPlan.joinToString(",")),
                        listOf(newShop.joinToString(",")),
                    )
                )
            },
            async {
                if (incrementCounter && removeIdFromPlan != null) {
                    val rangeRef = "$SHEET_NAME_RECIPES!D${removeIdFromPlan}"
                    val range = readSheetRange(rangeRef)
                    val oldCounter = range.elementAtOrNull(0)?.elementAtOrNull(0)?.toString()?.toLongOrNull()
                    val newCounter = ((oldCounter ?: 0) + 1).toString()
                    updateRawValue(rangeRef, listOf(listOf(newCounter)))
                }
            }
        )


    }
    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun updateRecipe(id: Long, title: String, url: String?, tags: Set<String>) {
    val state = RecipesState.value()
    val target = (ScreenState.value() as? ScreenState.Add)?.target
    ScreenState.set(target ?: ScreenState.Recipes)
    val tagString = tags.joinToString(SEPARATOR_TAGS)

    if (state is RecipesState.Success) {
        RecipesState.set(
            RecipesState.Success(
                state.recipes.map { if (it.id == id) Recipe(id, title, url, it.counter, tagString.ifBlank { null }) else it },
                state.plan,
                state.shop,
            )
        )
    }

    withContext(Dispatchers.IO) {
        writeRecipe(id, title, url, tagString)
    }

    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun addRecipe(title: String, url: String?, target: ScreenState.BaseScreen, tags: Set<String>) {
    ScreenState.set(target)

    val tagsString = tags.joinToString(SEPARATOR_TAGS)

    val state = RecipesState.value()
    if (state is RecipesState.Success) {
        RecipesState.set(
            RecipesState.Success(
                state.recipes + Recipe(ID_TEMPORARY, title, url, 0, tagsString),
                state.plan.let { if (target == ScreenState.Plan) it + ID_TEMPORARY else it },
                state.shop.let { if (target == ScreenState.Shop) it + ID_TEMPORARY else it },
            )
        )
    }

    withContext(Dispatchers.IO) {
        val recipes = readSheetRange(SHEET_NAME_RECIPES)

        val id = recipes.size + 1L
        writeRecipe(id, title, url, tagsString)

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
    val recipesDeferred = async { readRecipes() }
    val planDeferred = async { readPlanAndShop() }

    val recipes = recipesDeferred.await()
    val (plan, shop) = planDeferred.await()

    c.database.updateWith(recipes, plan, shop)
    withContext(Dispatchers.Default) { updateWidget(c.platformContext) }

    coroutineContext.statesOrNull?.setState(RecipesState, RecipesState.Success(recipes, plan, shop))
}

context(c: RecipeContext)
private fun readRecipes(): List<Recipe> {
    return readSheetRange(SHEET_NAME_RECIPES)
        .mapIndexedNotNull { i, row ->
            if (row.isNotEmpty() && row.elementAtOrNull(COLUMN_DELETED)?.toString() != DELETED_VALUE) {
                Recipe(
                    id = i + 1L,
                    title = row[COLUMN_NAME].toString().trim(),
                    url = row.elementAtOrNull(COLUMN_URL)?.toString()?.ifBlank { null },
                    counter = row.elementAtOrNull(COLUMN_COUNTER)?.toString()?.toLongOrNull() ?: 0,
                    tags = row.elementAtOrNull(COLUMN_TAGS)?.toString(),
                )
            } else {
                null
            }
        }
}

context(c: RecipeContext)
private fun writeRecipe(id: Long, title: String, url: String?, tagsString: String) {
    c.sheets.spreadsheets().values().batchUpdate(
        c.spreadSheetsId,
        BatchUpdateValuesRequest().apply {
            data = listOf(
                ValueRange().apply {
                    range = "$SHEET_NAME_RECIPES!A$id:B$id"
                    setValues(listOf(listOf(title, url ?: "")))
                    valueInputOption = "RAW"
                },
                ValueRange().apply {
                    range = "$SHEET_NAME_RECIPES!E$id"
                    setValues(listOf(listOf(tagsString)))
                    valueInputOption = "RAW"
                },
            )
        }
    ).execute()
}

context(c: RecipeContext)
private fun readPlanAndShop(): Pair<List<Long>, List<Long>> {
    val range = readSheetRange(RANGE_PLAN_AND_SHOP)
    return Pair(
        parsePlanCell(range.elementAtOrNull(0)?.elementAtOrNull(0)),
        parsePlanCell(range.elementAtOrNull(1)?.elementAtOrNull(0))
    )
}

private fun parsePlanCell(elementAtOrNull: Any?): List<Long> {
    return elementAtOrNull
        ?.toString()
        ?.split(",")
        ?.map { it.toLong() }
        .orEmpty()
}

context(c: RecipeContext)
private fun readSheetRange(range: String): List<List<Any?>> {
    return c.sheets.spreadsheets().values()
        .get(c.spreadSheetsId, range)
        .execute()
        .getValues()
        ?: emptyList()
}

private fun Database.updateWith(recipes: List<Recipe>, plan: List<Long>, shop: List<Long>) {
    transaction {
        recipesQueries.deleteAll()
        recipes.forEach {
            recipesQueries.insert(it.id, it.title, it.url, it.counter, it.tags)
        }

        planQueries.deleteAll()
        plan.forEach {
            planQueries.insert(it)
        }

        shopQueries.deleteAll()
        shop.forEach {
            shopQueries.insert(it)
        }
    }
}

private suspend fun setStateFromDatabase(database: Database) {
    val recipes = database.recipesQueries.selectAll().executeAsList()
    RecipesState.set(
        if (recipes.isNotEmpty()) {
            RecipesState.Success(
                recipes = recipes,
                plan = database.planQueries.selectAll().executeAsList(),
                shop = database.shopQueries.selectAll().executeAsList(),
            )
        } else {
            RecipesState.Loading
        }
    )
}
