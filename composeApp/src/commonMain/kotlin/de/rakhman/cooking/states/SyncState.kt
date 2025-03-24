package de.rakhman.cooking.states

import io.sellmair.evas.State
import io.sellmair.evas.set

enum class SyncState(val isSyncing: Boolean) : State {
    Syncing(true), NotSyncing(false);

    companion object : State.Key<SyncState> {
        override val default: SyncState get() = NotSyncing

        suspend fun setSyncing() = set(Syncing)
        suspend fun setNotSyncing() = set(NotSyncing)
    }
}