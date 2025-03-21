@file:Suppress("UsePropertyAccessSyntax")

package de.rakhman.cooking.states

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import de.rakhman.cooking.Database
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.events.DeleteEvent
import de.rakhman.cooking.events.ErrorEvent
import de.rakhman.cooking.events.ReloadEvent
import io.sellmair.evas.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class RecipesState : State {
    data object Loading : RecipesState()
    data class Success(val list: List<Recipe>) : RecipesState()

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
    loadFromDatabase(database)

    with(RecipeContext(database, sheets, spreadSheetsId)) {
        collectEventsAsync<ReloadEvent>(Dispatchers.IO) { syncWithSheets() }
        collectEventsAsync<DeleteEvent>(Dispatchers.IO) { event ->
            try {
                setDeleted(event.id)
                ReloadEvent.emit()
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorEvent(e).emit()
            }
        }
    }

    ReloadEvent.emit()
}

context(c: RecipeContext)
private fun setDeleted(id: Long) {
    c.sheets.spreadsheets().values().update(
        c.spreadSheetsId,
        "$SHEET_NAME_RECIPES!C${id + 1}",
        ValueRange().apply {
            setValues(listOf(listOf(DELETED_VALUE)))
        }
    ).run {
        valueInputOption = "RAW"
        execute()
    }
}

context(c: RecipeContext)
private suspend fun StateProducerScope<RecipesState>.syncWithSheets() {
    try {
        val recipes = c.sheets.spreadsheets().values()
            .get(c.spreadSheetsId, SHEET_NAME_RECIPES)
            .execute()
            .getValues()
            .mapIndexedNotNull { i, row ->
                if (row.isNotEmpty() && row.elementAtOrNull(2)?.toString() != DELETED_VALUE) {
                    Recipe(i.toLong(), row[0].toString(), row.elementAtOrNull(1)?.toString())
                } else {
                    null
                }
            }

        c.database.updateWith(recipes)

        withContext(Dispatchers.Main) {
            emit(RecipesState.Success(recipes))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        ErrorEvent(e).emit()
    }
}

private fun Database.updateWith(recipes: List<Recipe>) {
    transaction {
        recipesQueries.deleteAll()
        recipes.forEach {
            recipesQueries.insert(it.id, it.title, it.url)
        }
    }
}

private suspend fun StateProducerScope<RecipesState>.loadFromDatabase(database: Database) {
    val recipes = database.recipesQueries.selectAll().executeAsList()
    if (recipes.isNotEmpty()) {
        emit(RecipesState.Success(recipes))
    } else {
        emit(RecipesState.Loading)
    }
}
