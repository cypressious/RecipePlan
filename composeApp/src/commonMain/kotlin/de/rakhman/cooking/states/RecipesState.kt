package de.rakhman.cooking.states

import de.rakhman.cooking.Recipe
import de.rakhman.cooking.events.ReloadEvent
import io.sellmair.evas.State
import io.sellmair.evas.StateProducerScope
import io.sellmair.evas.collectEvents
import io.sellmair.evas.launchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

sealed class RecipesState : State {
    data object Loading : RecipesState()
    data object Error : RecipesState()
    data class Success(val list: List<Recipe>) : RecipesState()

    companion object Key : State.Key<RecipesState> {
        override val default: RecipesState get() = Loading
    }
}

fun CoroutineScope.launchDummyRecipesState() = launchState(RecipesState) {
    collectEvents<ReloadEvent> { doReload() }
}

private suspend fun StateProducerScope<RecipesState>.doReload() {
    emit(RecipesState.Loading)
    delay(2_000)
    val dummyData = List(100) { Recipe(it.toString(), "Rezept $it", "https://kotlinlang.org") }
    emit(RecipesState.Success(dummyData))
}
