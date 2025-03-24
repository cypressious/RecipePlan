package de.rakhman.cooking.states

import io.sellmair.evas.State

sealed class ScreenState(val title: String) : State {
    sealed class BaseScreen(title: String) : ScreenState(title)
    data object Recipes : BaseScreen("Rezepte")
    data object Plan : BaseScreen("Plan")
    data object Shop : BaseScreen("Einkaufen")
    class Add(val target: BaseScreen, val initialData: String? = null) : ScreenState("Rezept Hinzufügen")
    companion object Key : State.Key<ScreenState> {
        override val default: ScreenState
            get() = Plan
    }
}