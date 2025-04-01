package de.rakhman.cooking.states

import io.sellmair.evas.*

class SettingsState(val spreadSheetsId: String) : State {
    companion object : State.Key<SettingsState?> {
        override val default: SettingsState?
            get() = null
    }
}