@file:Suppress("UsePropertyAccessSyntax")

package de.rakhman.cooking.states

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import de.rakhman.cooking.Database
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.events.AddEvent
import de.rakhman.cooking.events.AddToPlanEvent
import de.rakhman.cooking.events.DeleteEvent
import de.rakhman.cooking.events.ErrorEvent
import de.rakhman.cooking.events.ReloadEvent
import de.rakhman.cooking.events.RemoveFromPlanEvent
import io.sellmair.evas.*
import io.sellmair.evas.compose.eventsOrThrow
import io.sellmair.evas.compose.statesOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

sealed class RecipesState : State {
    data object Loading : RecipesState()
    data class Success(val recipes: List<Recipe>, val plan: List<Long>) : RecipesState()

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
    loadFromDatabase(database).emit()

    with(RecipeContext(database, sheets, spreadSheetsId)) {
        collectEventsAsyncOnIoCatchingErrors<ReloadEvent> { syncWithSheets() }
        collectEventsAsyncOnIoCatchingErrors<DeleteEvent> { setDeleted(it.id) }
        collectEventsAsyncOnIoCatchingErrors<AddEvent> { addRecipe(it.title, it.url) }
        collectEventsAsyncOnIoCatchingErrors<AddToPlanEvent> { addtoPlan(it.id) }
        collectEventsAsyncOnIoCatchingErrors<RemoveFromPlanEvent> { removeFromPlan(it.index) }
    }

    ReloadEvent.emit()
}

private inline fun <reified T : Event> StateProducerScope<RecipesState>.collectEventsAsyncOnIoCatchingErrors(
    crossinline f: suspend (T) -> Unit
) {
    collectEventsAsync<T> { event ->
        try {
            f(event)
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorEvent(e).emit()
        }
    }
}

context(c: RecipeContext)
private suspend fun setDeleted(id: Long) {
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
private suspend fun addtoPlan(id: Long) {
    val state = RecipesState.value() as? RecipesState.Success ?: return
    val rawPlanValue = (state.plan + id).joinToString(",")
    withContext(Dispatchers.IO) { updateRawValue("$SHEET_NAME_PLAN!A1", listOf(listOf(rawPlanValue))) }
    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun removeFromPlan(indexToRemove: Int) {
    val state = RecipesState.value() as? RecipesState.Success ?: return
    val rawPlanValue = state.plan.filterIndexed { i, _ -> i != indexToRemove }.joinToString(",")
    withContext(Dispatchers.IO) { updateRawValue("$SHEET_NAME_PLAN!A1", listOf(listOf(rawPlanValue))) }
    syncWithSheets()
}

context(c: RecipeContext)
private suspend fun addRecipe(title: String, url: String?) {
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

    ScreenState.set(ScreenState.Recipes)
}

context(c: RecipeContext)
private suspend fun syncWithSheets() = withContext(Dispatchers.IO) {
    val recipesDeferred = async {
        readSheetRange(SHEET_NAME_RECIPES)
            .mapIndexedNotNull { i, row ->
                if (row.isNotEmpty() && row.elementAtOrNull(2)?.toString() != DELETED_VALUE) {
                    Recipe(i + 1L, row[0].toString(), row.elementAtOrNull(1)?.toString()?.ifBlank { null })
                } else {
                    null
                }
            }
    }

    val planDeferred = async {
        readSheetRange(SHEET_NAME_PLAN)
            .elementAtOrNull(0)
            ?.elementAtOrNull(0)
            ?.toString()
            ?.split(",")
            ?.map { it.toLong() }
            .orEmpty()
    }

    val recipes = recipesDeferred.await()
    val plan = planDeferred.await()

    c.database.updateWith(recipes, plan)

    RecipesState.set(RecipesState.Success(recipes, plan))
}

context(c: RecipeContext)
private fun readSheetRange(range: String): List<List<Any?>> {
    return c.sheets.spreadsheets().values()
        .get(c.spreadSheetsId, range)
        .execute()
        .getValues()
}

private fun Database.updateWith(recipes: List<Recipe>, plan: List<Long>) {
    transaction {
        recipesQueries.deleteAll()
        recipes.forEach {
            recipesQueries.insert(it.id, it.title, it.url)
        }

        planQueries.deleteAll()
        plan.forEach {
            planQueries.insert(it)
        }
    }
}

private fun loadFromDatabase(database: Database): RecipesState {
    val recipes = database.recipesQueries.selectAll().executeAsList()
    val plan = database.planQueries.selectAll().executeAsList()
    return if (recipes.isNotEmpty()) {
        RecipesState.Success(recipes, plan)
    } else {
        RecipesState.Loading
    }
}
