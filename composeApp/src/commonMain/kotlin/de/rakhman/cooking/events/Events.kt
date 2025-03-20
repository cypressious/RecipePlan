package de.rakhman.cooking.events

import io.sellmair.evas.Event

object ReloadEvent : Event

class DeleteEvent(val id: String) : Event