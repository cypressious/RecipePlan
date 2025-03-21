package de.rakhman.cooking.events

import io.sellmair.evas.Event

object ReloadEvent : Event

class DeleteEvent(val id: Long) : Event

class AddToPlanEvent(val id: Long) : Event

class RemoveFromPlanEvent(val index: Int) : Event

class AddEvent(val title: String, val url: String?) : Event

class ErrorEvent(e: Exception) : Event