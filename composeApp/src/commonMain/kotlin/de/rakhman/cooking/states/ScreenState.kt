package de.rakhman.cooking.states

import io.sellmair.evas.State

sealed class ScreenState(val title: String) : State {
    data object Recipes : ScreenState("Rezepte")
    data object Plan : ScreenState("Plan")
    companion object Key : State.Key<ScreenState> {
        override val default: ScreenState
            get() = Recipes
    }
}