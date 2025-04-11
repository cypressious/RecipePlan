package de.rakhman.cooking.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.*
import de.rakhman.cooking.events.*
import de.rakhman.cooking.states.ID_TEMPORARY
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.emitAsync
import io.sellmair.evas.set
import io.sellmair.evas.value
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import recipeplan.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeItem(recipe: Recipe, screenState: ScreenState, slotLeft: (@Composable RowScope.() -> Unit)? = null) {
    val context = getContext()
    Row(
        modifier = Modifier
        .height(IntrinsicSize.Min)
        .clickable(onClick = {
            recipe.url?.let { openUrl(it, context) }
        })
    ) {
        slotLeft?.invoke(this)

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = if (slotLeft == null) 16.dp else 0.dp, end = 16.dp, top = 8.dp, bottom = 8.dp).weight(1f),
        ) {
            Text(text = recipe.title, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
            recipe.url?.let {
                Text(
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (recipe.id != ID_TEMPORARY) {
                IconButton(onClick = { expanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.actions))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (screenState == ScreenState.Recipes) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.add_to_shop)) },
                            onClick = EvasLaunching {
                                NotificationEvent(getString(Res.string.recipe_added_to_shop, recipe.title)).emitAsync()
                                AddToShopEvent(recipe.id).emitAsync()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.add_to_plan)) },
                            onClick = EvasLaunching {
                                NotificationEvent(getString(Res.string.recipe_added_to_plan, recipe.title)).emitAsync()
                                AddToPlanEvent(recipe.id).emitAsync()
                                expanded = false
                            }
                        )
                    }
                    if (screenState == ScreenState.Shop) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.remove_from_shop)) },
                            onClick = EvasLaunching {
                                NotificationEvent(getString(Res.string.recipe_removed_from_shop, recipe.title))
                                    .emitAsync()
                                RemoveFromShopEvent(recipe.id).emitAsync()
                                expanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.edit)) },
                        onClick = EvasLaunching {
                            ScreenState.set(
                                ScreenState.Add(
                                    target = ScreenState.value() as ScreenState.BaseScreen,
                                    editingRecipe = recipe
                                )
                            )
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.delete)) },
                        onClick = EvasLaunching {
                            DeleteRequestEvent(recipe).emitAsync()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.share)) },
                        onClick = { shareRecipe(recipe.title, recipe.url, context); expanded = false }
                    )
                    recipe.url?.let { url ->
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.add_to_bring)) },
                            onClick = { shareToBring(recipe.title, url, context); expanded = false }
                        )
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp).width(24.dp).height(24.dp))
            }
        }
    }
}