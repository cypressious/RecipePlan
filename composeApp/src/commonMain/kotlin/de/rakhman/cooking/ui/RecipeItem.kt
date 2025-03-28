package de.rakhman.cooking.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.events.AddToPlanEvent
import de.rakhman.cooking.events.AddToShopEvent
import de.rakhman.cooking.events.DeleteEvent
import de.rakhman.cooking.events.NotificationEvent
import de.rakhman.cooking.events.RemoveFromShopEvent
import de.rakhman.cooking.getContext
import de.rakhman.cooking.openUrl
import de.rakhman.cooking.shareRecipe
import de.rakhman.cooking.shareToBring
import de.rakhman.cooking.states.ID_TEMPORARY
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.emitAsync
import io.sellmair.evas.set
import io.sellmair.evas.value

@Composable
fun RecipeItem(recipe: Recipe, screenState: ScreenState) {
    val context = getContext()
    Row(modifier = Modifier.Companion.clickable(onClick = {
        recipe.url?.let { openUrl(it, context) }
    })) {
        Column(
            modifier = Modifier.Companion.fillMaxWidth().padding(16.dp, 8.dp).weight(1f),
        ) {
            Text(text = recipe.title, fontSize = 18.sp, modifier = Modifier.Companion.padding(bottom = 8.dp))
            recipe.url?.let {
                Text(
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis,
                    color = Color.Companion.Gray
                )
            }
        }

        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.Companion.align(Alignment.Companion.CenterVertically).padding(end = 12.dp),
            contentAlignment = Alignment.Companion.Center
        ) {
            if (recipe.id != ID_TEMPORARY) {
                IconButton(onClick = { expanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Optionen")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (screenState == ScreenState.Recipes) {
                        DropdownMenuItem(
                            text = { Text("Auf die Einkaufsliste") },
                            onClick = EvasLaunching {
                                NotificationEvent("\"${recipe.title}\" zur Einkaufsliste hinzugefügt.").emitAsync()
                                AddToShopEvent(recipe.id).emitAsync()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Auf den Plan") },
                            onClick = EvasLaunching {
                                NotificationEvent("\"${recipe.title}\" zum Plan hinzugefügt.").emitAsync()
                                AddToPlanEvent(recipe.id).emitAsync()
                                expanded = false
                            }
                        )
                    }
                    if (screenState == ScreenState.Shop) {
                        DropdownMenuItem(
                            text = { Text("Von Einkaufsliste entfernen") },
                            onClick = EvasLaunching {
                                NotificationEvent("\"${recipe.title}\" von Einkaufsliste entfernt.").emitAsync()
                                RemoveFromShopEvent(recipe.id).emitAsync()
                                expanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Bearbeiten") },
                        onClick = EvasLaunching {
                            ScreenState.Key.set(
                                ScreenState.Add(
                                    target = ScreenState.Key.value() as ScreenState.BaseScreen,
                                    editingRecipe = recipe
                                )
                            )
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Löschen") },
                        onClick = EvasLaunching {
                            NotificationEvent("\"${recipe.title}\" gelöscht.").emitAsync()
                            DeleteEvent(recipe.id).emitAsync()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Teilen") },
                        onClick = { shareRecipe(recipe.title, recipe.url, context); expanded = false }
                    )
                    recipe.url?.let { url ->
                        DropdownMenuItem(
                            text = { Text("Zu Bring hinzufügen") },
                            onClick = { shareToBring(recipe.title, url, context); expanded = false }
                        )
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.Companion.padding(12.dp).width(24.dp).height(24.dp))
            }
        }
    }
}