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
private const val DELETED_VALUE = "deleted"

private class RecipeContext(
    val database: Database,
    val sheets: Sheets,
    val spreadSheetsId: String
)

fun CoroutineScope.launchRecipesState(
    database: Database, sheets: Sheets, spreadSheetsId: String = "XXX"
) = launchState(RecipesState) {
    with(RecipeContext(database, sheets, spreadSheetsId)) {
        loadFromDatabase()
        collectEventsAsyncCatchingErrors<ReloadEvent> { syncWithSheets() }
        collectEventsAsyncCatchingErrors<DeleteEvent> { setDeleted(it.id) }
        collectEventsAsyncCatchingErrors<AddEvent> { addRecipe(it.title, it.url) }
        collectEventsAsyncCatchingErrors<AddToPlanEvent> { addToPlan(it.id, it.removeIndexFromShop) }
        collectEventsAsyncCatchingErrors<RemoveFromPlanEvent> { removeFromPlan(it.index) }
        collectEventsAsyncCatchingErrors<AddToShopEvent> { addToShop(it.id) }
        collectEventsAsyncCatchingErrors<RemoveFromShopEvent> { removeFromShop(it.index) }
    }

    ReloadEvent.emit()
}

context(c: RecipeContext)
private inline fun <reified T : Event> StateProducerScope<RecipesState>.collectEventsAsyncCatchingErrors(
    crossinline f: suspend (T) -> Unit
) {
    collectEventsAsync<T> { event ->
        try {
            f(event)
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorEvent(e).emit()
            loadFromDatabase()
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
private suspend fun addToPlan(id: Long, removeIndexFromShop: Int?) {
    val state = RecipesState.value() as? RecipesState.Success ?: return
    val newPlan = state.plan + id
    val newShop = if (removeIndexFromShop != null) {
        state.shop.filterIndexed { i, _ -> i != removeIndexFromShop }
    } else {
        state.shop
    }
    RecipesState.set(RecipesState.Success(state.recipes, newPlan, newShop))
    withContext(Dispatchers.IO) {
        updateRawValue(
            "$SHEET_NAME_PLAN!A1:A2",
            listOf(
                listOf(newPlan.joinToString(",")),
                listOf(newShop.joinToString(",")),
            )
        )
    }
    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun addToShop(id: Long) {
    val state = RecipesState.value() as? RecipesState.Success ?: return
    val newShop = state.shop + id
    RecipesState.set(RecipesState.Success(state.recipes, state.plan, newShop))
    val rawPlanValue = newShop.joinToString(",")
    withContext(Dispatchers.IO) { updateRawValue("$SHEET_NAME_PLAN!A2", listOf(listOf(rawPlanValue))) }
    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun removeFromPlan(indexToRemove: Int) {
    val state = RecipesState.value() as? RecipesState.Success ?: return
    val filteredPlan = state.plan.filterIndexed { i, _ -> i != indexToRemove }
    RecipesState.set(RecipesState.Success(state.recipes, filteredPlan, state.shop))
    val rawPlanValue = filteredPlan.joinToString(",")
    withContext(Dispatchers.IO) { updateRawValue("$SHEET_NAME_PLAN!A1", listOf(listOf(rawPlanValue))) }
    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun removeFromShop(indexToRemove: Int) {
    val state = RecipesState.value() as? RecipesState.Success ?: return
    val filteredShop = state.shop.filterIndexed { i, _ -> i != indexToRemove }
    RecipesState.set(RecipesState.Success(state.recipes, state.plan, filteredShop))
    val rawPlanValue = filteredShop.joinToString(",")
    withContext(Dispatchers.IO) { updateRawValue("$SHEET_NAME_PLAN!A2", listOf(listOf(rawPlanValue))) }
    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun addRecipe(title: String, url: String?) {
    val state = RecipesState.value()
    if (state is RecipesState.Success) {
        RecipesState.set(
            RecipesState.Success(
                state.recipes + Recipe(Long.MAX_VALUE, title, url),
                state.plan,
                state.shop,
            )
        )
    }
    ScreenState.set(ScreenState.Recipes)

    withContext(Dispatchers.IO) {
        val recipes = readSheetRange(SHEET_NAME_RECIPES)

        val id = recipes.size + 1
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

        syncWithSheets()
    }
}

context(c: RecipeContext)
private suspend fun syncWithSheets() = withContext(Dispatchers.IO) {
    val recipesDeferred = async {
        readSheetRange(SHEET_NAME_RECIPES)
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

    val planDeferred = async {
        val range = readSheetRange(SHEET_NAME_PLAN)
        Pair(
            parsePlanCell(
                range
                    .elementAtOrNull(0)?.elementAtOrNull(0)
            ),
            parsePlanCell(
                range
                    .elementAtOrNull(1)?.elementAtOrNull(0)
            )
        )
    }

    val recipes = recipesDeferred.await()
    val (plan, shop) = planDeferred.await()

    c.database.updateWith(recipes, plan, shop)

    RecipesState.set(RecipesState.Success(recipes, plan, shop))
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
private suspend fun loadFromDatabase() {
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
