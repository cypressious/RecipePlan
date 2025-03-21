package de.rakhman.cooking.events

import io.sellmair.evas.Event

object ReloadEvent : Event

class DeleteEvent(val id: Long) : Event

class ErrorEvent(e: Exception) : Event