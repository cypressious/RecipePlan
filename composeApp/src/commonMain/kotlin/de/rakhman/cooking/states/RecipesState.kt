@file:Suppress("UsePropertyAccessSyntax")

package de.rakhman.cooking.states

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import de.rakhman.cooking.Database
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.events.AddEvent
import de.rakhman.cooking.events.AddToPlanEvent
import de.rakhman.cooking.events.AddToShopEvent
import de.rakhman.cooking.events.DeleteEvent
import de.rakhman.cooking.events.ErrorEvent
import de.rakhman.cooking.events.ReloadEvent
import de.rakhman.cooking.events.RemoveFromPlanEvent
import de.rakhman.cooking.events.RemoveFromShopEvent
import de.rakhman.cooking.events.UpdateEvent
import io.sellmair.evas.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

sealed class RecipesState : State {
    data object Loading : RecipesState()
    data class Success(val recipes: List<Recipe>, val plan: List<Long>, val shop: List<Long>) : RecipesState()

    companion object Key : State.Key<RecipesState> {
        override val default: RecipesState get() = Loading
    }
}

private const val SHEET_NAME_RECIPES = "Rezepte"
private const val SHEET_NAME_PLAN = "Plan"
private const val RANGE_PLAN_AND_SHOP = "$SHEET_NAME_PLAN!A1:A2"
private const val DELETED_VALUE = "deleted"

const val ID_TEMPORARY = -1L

private class RecipeContext(
    val database: Database,
    val sheets: Sheets,
    val spreadSheetsId: String
)

fun CoroutineScope.launchRecipesState(
    database: Database, sheets: Sheets, spreadSheetsId: String = "XXX"
) = launchState(RecipesState) {
    with(RecipeContext(database, sheets, spreadSheetsId)) {
        setStateFromDatabase()
        collectEventsAsyncCatchingErrors<ReloadEvent> { syncWithSheets() }
        collectEventsAsyncCatchingErrors<DeleteEvent> { setDeleted(it.id) }
        collectEventsAsyncCatchingErrors<AddEvent> { addRecipe(it.title, it.url) }
        collectEventsAsyncCatchingErrors<UpdateEvent> { updateRecipe(it.id, it.title, it.url) }
        collectEventsAsyncCatchingErrors<AddToPlanEvent> {
            updatePlanAndShop(addIdToPlan = it.id, removeIndexFromShop = it.removeIndexFromShop)
        }
        collectEventsAsyncCatchingErrors<RemoveFromPlanEvent> {
            updatePlanAndShop(removeIndexFromPlan = it.index)
        }
        collectEventsAsyncCatchingErrors<AddToShopEvent> {
            updatePlanAndShop(addIdToShop = it.id)
        }
        collectEventsAsyncCatchingErrors<RemoveFromShopEvent> {
            updatePlanAndShop(removeIndexFromShop = it.index)
        }
    }

    ReloadEvent.emit()
}

context(c: RecipeContext)
private inline fun <reified T : Event> StateProducerScope<RecipesState>.collectEventsAsyncCatchingErrors(
    crossinline f: suspend (T) -> Unit
) {
    collectEventsAsync<T> { event ->
        try {
            SyncState.setSyncing()
            f(event)
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorEvent(e).emit()
            setStateFromDatabase()
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
    removeIndexFromPlan: Int? = null,
    removeIndexFromShop: Int? = null,
    updateStateOptimistically: Boolean = true,
) {
    fun newPlanAndShop(
        plan: List<Long>,
        shop: List<Long>,
    ): Pair<List<Long>, List<Long>> {
        fun List<Long>.updatePlan(addId: Long?, removeIndex: Int?): List<Long> {
            return buildList {
                this@updatePlan.filterIndexedTo(this) { i, id -> i != removeIndex && id != ID_TEMPORARY }
                addId?.let { add(it) }
            }
        }

        val newPlan = plan.updatePlan(addIdToPlan, removeIndexFromPlan)
        val newShop = shop.updatePlan(addIdToShop, removeIndexFromShop)
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
        val (oldPlan, oldShop) = readPlanAndShop()
        val (newPlan, newShop) = newPlanAndShop(oldPlan, oldShop)

        updateRawValue(
            RANGE_PLAN_AND_SHOP,
            listOf(
                listOf(newPlan.joinToString(",")),
                listOf(newShop.joinToString(",")),
            )
        )
    }
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
                state.recipes.map { if (it.id == id) Recipe(id, title, url) else it },
                state.plan,
                state.shop,
            )
        )
    }

    withContext(Dispatchers.IO) {
        c.sheets.spreadsheets().values().update(
            c.spreadSheetsId,
            "$SHEET_NAME_RECIPES!A$id:B$id",
            ValueRange().apply {
                setValues(listOf(listOf(title, url ?: "")))
            }
        ).run {
            valueInputOption = "RAW"
            execute()
        }
    }

    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun addRecipe(title: String, url: String?) {
    val state = RecipesState.value()
    val target = (ScreenState.value() as? ScreenState.Add)?.target
    ScreenState.set(target ?: ScreenState.Recipes)

    if (state is RecipesState.Success) {
        RecipesState.set(
            RecipesState.Success(
                state.recipes + Recipe(ID_TEMPORARY, title, url),
                state.plan.let { if (target == ScreenState.Plan) it + ID_TEMPORARY else it },
                state.shop.let { if (target == ScreenState.Shop) it + ID_TEMPORARY else it },
            )
        )
    }

    withContext(Dispatchers.IO) {
        val recipes = readSheetRange(SHEET_NAME_RECIPES)

        val id = recipes.size + 1L
        c.sheets.spreadsheets().values().update(
            c.spreadSheetsId,
            "$SHEET_NAME_RECIPES!A$id:B$id",
            ValueRange().apply {
                setValues(listOf(listOf(title, url ?: "")))
            }
        ).run {
            valueInputOption = "RAW"
            execute()
        }

        if (target == ScreenState.Shop) {
            updatePlanAndShop(
                addIdToShop = id,
                updateStateOptimistically = false
            )
        } else if (target == ScreenState.Plan) {
            updatePlanAndShop(
                addIdToPlan = id,
                removeIndexFromShop = null,
                updateStateOptimistically = false
            )
        }
    }

    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun syncWithSheets() = withContext(Dispatchers.IO) {
    val recipesDeferred = async { readRecipes() }
    val planDeferred = async { readPlanAndShop() }

    val recipes = recipesDeferred.await()
    val (plan, shop) = planDeferred.await()

    c.database.updateWith(recipes, plan, shop)

    RecipesState.set(RecipesState.Success(recipes, plan, shop))
}

context(c: RecipeContext)
private fun readRecipes(): List<Recipe> {
    return readSheetRange(SHEET_NAME_RECIPES)
        .mapIndexedNotNull { i, row ->
            if (row.isNotEmpty() && row.elementAtOrNull(2)?.toString() != DELETED_VALUE) {
                Recipe(
                    id = i + 1L,
                    title = row[0].toString(),
                    url = row.elementAtOrNull(1)?.toString()?.ifBlank { null })
            } else {
                null
            }
        }
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
            recipesQueries.insert(it.id, it.title, it.url)
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

context(c: RecipeContext)
private suspend fun setStateFromDatabase() {
    RecipesState.set(
        if (c.database.recipesQueries.selectAll().executeAsList().isNotEmpty()) {
            RecipesState.Success(
                recipes = c.database.recipesQueries.selectAll().executeAsList(),
                plan = c.database.planQueries.selectAll().executeAsList(),
                shop = c.database.shopQueries.selectAll().executeAsList(),
            )
        } else {
            RecipesState.Loading
        }
    )
}
