package de.rakhman.cooking.events

import de.rakhman.cooking.Recipe
import io.sellmair.evas.Event

object ReloadEvent : Event

class DeleteEvent(val id: Long) : Event

class AddToPlanEvent(val id: Long) : Event

class AddToShopEvent(val id: Long) : Event

class RemoveFromPlanEvent(val id: Long) : Event

class RemoveFromShopEvent(val id: Long) : Event

class AddEvent(val title: String, val url: String?) : Event

class UpdateEvent(val id: Long, val title: String, val url: String?) : Event

class ErrorEvent(val e: Exception) : Event

class NotificationEvent(val message: String) : Event

class DeleteRequestEvent(val recipe: Recipe) : Event

class SpreadsheetIdChangedEvent(val id: String?) : Event

object CreateSpreadsheetEvent : Event