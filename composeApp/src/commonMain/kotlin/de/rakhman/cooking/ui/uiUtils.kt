package de.rakhman.cooking.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.sellmair.evas.Event
import io.sellmair.evas.compose.eventsOrThrow

@Composable
inline fun <reified T : Event> collectEventsComposable(noinline cb: suspend (T) -> Unit) {
    val events = eventsOrThrow()
    LaunchedEffect(true) {
        events.events(T::class).collect(cb)
    }
}