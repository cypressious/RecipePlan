package de.rakhman.cooking.events

import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.Event

object ReloadEvent : Event

class DeleteEvent(val id: Long) : Event

class AddToPlanEvent(val id: Long) : Event

class AddToShopEvent(val id: Long) : Event

class RemoveFromPlanEvent(val id: Long, val incrementCounter: Boolean) : Event

class RemoveFromShopEvent(val id: Long) : Event

class AddEvent(val title: String, val url: String?, val tags: Set<String>, val target: ScreenState.BaseScreen, val text: String? = null) : Event

class UpdateEvent(val id: Long, val title: String, val url: String?, val tags: Set<String>, val text: String? = null) : Event

class ErrorEvent(val e: Exception) : Event

class NotificationEvent(val message: String) : Event

class DeleteRequestEvent(val recipe: RecipeDto) : Event

class SpreadsheetIdChangedEvent(val id: String?) : Event

object CreateSpreadsheetEvent : Event

class ChangeScreenEvent(val screen: ScreenState) : Event