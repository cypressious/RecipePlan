package de.rakhman.cooking.states

import io.sellmair.evas.State

class SettingsState(val spreadSheetsId: String) : State {
    companion object : State.Key<SettingsState?> {
        override val default: SettingsState?
            get() = null
    }
}

enum class SavingSettingsState : State {
    NotSaving, Saving;
    companion object : State.Key<SavingSettingsState> {
        override val default: SavingSettingsState
            get() = NotSaving
    }
}