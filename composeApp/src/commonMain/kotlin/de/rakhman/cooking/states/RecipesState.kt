package de.rakhman.cooking.states

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.Request
import de.rakhman.cooking.Database
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.events.DeleteEvent
import de.rakhman.cooking.events.ReloadEvent
import io.sellmair.evas.State
import io.sellmair.evas.StateProducerScope
import io.sellmair.evas.collectEvents
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.emit
import io.sellmair.evas.launchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed class RecipesState : State {
    data object Loading : RecipesState()
    data class Success(val list: List<Recipe>) : RecipesState()

    companion object Key : State.Key<RecipesState> {
        override val default: RecipesState get() = Loading
    }
}

fun CoroutineScope.launchDummyRecipesState() = launchState(RecipesState) {
    collectEvents<ReloadEvent> {
        emit(RecipesState.Loading)
        delay(2_000)
        val dummyData = List(100) { Recipe(it.toString(), "Rezept $it", "https://kotlinlang.org") }
        emit(RecipesState.Success(dummyData))
    }
}

fun CoroutineScope.launchRecipesState(
    database: Database,
    sheets: Sheets,
    spreadSheetsId: String = "XXX"
) = launchState(RecipesState) {
    loadFromDatabase(database)

    collectEventsAsync<ReloadEvent>(Dispatchers.IO) {
        syncWithSheets(database, sheets, spreadSheetsId)
    }
    collectEventsAsync<DeleteEvent>(Dispatchers.IO) { event ->
        try {
            val spreadsheet = sheets.spreadsheets().get(spreadSheetsId).execute()
            val sheetId = spreadsheet.sheets.firstOrNull { it.properties.title == "Rezepte" }
                ?.properties?.sheetId
                ?: return@collectEventsAsync
            val index = sheets.loadRecipes(spreadSheetsId).indexOfFirst { it.elementAtOrNull(0).toString() == event.id }
            if (index >= 0) {
                sheets.spreadsheets().batchUpdate(spreadSheetsId, BatchUpdateSpreadsheetRequest().apply {
                    requests = listOf(Request().apply {
                        deleteDimension = DeleteDimensionRequest().apply {
                            range = DimensionRange().apply {
                                this.sheetId = sheetId
                                dimension = "ROWS"
                                startIndex = index
                                endIndex = index + 1
                            }
                        }
                    })
                }).execute()
                ReloadEvent.emit()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO
        }
    }

    ReloadEvent.emit()
}

private suspend fun StateProducerScope<RecipesState>.syncWithSheets(
    database: Database,
    sheets: Sheets,
    spreadSheetsId: String
) {
    try {
        val recipes = sheets.loadRecipes(spreadSheetsId)
            .filter { it.size >= 2 }
            .map { it -> Recipe(it[0].toString(), it[1].toString(), it.elementAtOrNull(2)?.toString()) }
            .distinctBy { it.id }

        database.transaction {
            database.recipesQueries.deleteAll()
            recipes.forEach {
                database.recipesQueries.insert(it.id, it.title, it.url)
            }
        }

        withContext(Dispatchers.Main) {
            emit(RecipesState.Success(recipes))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // TODO
    }
}

private fun Sheets.loadRecipes(
    spreadSheetsId: String
): List<List<Any?>> = spreadsheets()
    .values()
    .get(spreadSheetsId, "Rezepte")
    .execute()
    .getValues()

private suspend fun StateProducerScope<RecipesState>.loadFromDatabase(database: Database) {
    val recipes = database.recipesQueries.selectAll().executeAsList()
    if (recipes.isNotEmpty()) {
        emit(RecipesState.Success(recipes))
    } else {
        emit(RecipesState.Loading)
    }
}

