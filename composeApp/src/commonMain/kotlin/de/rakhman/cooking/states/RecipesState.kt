package de.rakhman.cooking.states

import com.google.api.services.sheets.v4.Sheets
import de.rakhman.cooking.Database
import de.rakhman.cooking.Recipe
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

    ReloadEvent.emit()
}

private suspend fun StateProducerScope<RecipesState>.syncWithSheets(database: Database, sheets: Sheets, spreadSheetsId: String) {
    try {
        val valueRange = sheets.spreadsheets().values().get(spreadSheetsId, "Rezepte").execute()
        val recipes = valueRange.getValues()
            .filter { it.size >= 2 }
            .map { it -> Recipe(it[0].toString(), it[1].toString(), it.elementAtOrNull(2)?.toString()) }
            .distinctBy { it.id }
        database.transaction {
            database.recipesQueries.deleteAll()
            recipes.forEach {
                database.recipesQueries.insert(it.id, it.title, it.url)
            }
        }
        emit(RecipesState.Success(recipes))
    } catch (e: Exception) {
        e.printStackTrace()
        // TODO
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

