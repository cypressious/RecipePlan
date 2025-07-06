package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.*
import de.rakhman.cooking.getContext
import de.rakhman.cooking.shareRecipe
import de.rakhman.cooking.shareToBring
import de.rakhman.cooking.states.ID_TEMPORARY
import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.emitAsync
import io.sellmair.evas.set
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import recipeplan.composeapp.generated.resources.*

@Composable
fun RecipeDropdown(
    recipe: RecipeDto,
    screen: ScreenState.BaseScreen
) {
    var expanded by remember { mutableStateOf(false) }
    val context = getContext()

    Box(
        modifier = Modifier.padding(end = 12.dp),
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
                if (screen == ScreenState.Recipes) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.add_to_shop)) },
                        leadingIcon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                        onClick = EvasLaunching {
                            NotificationEvent(getString(Res.string.recipe_added_to_shop, recipe.title)).emitAsync()
                            AddToShopEvent(recipe.id).emitAsync()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.add_to_plan)) },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        onClick = EvasLaunching {
                            NotificationEvent(getString(Res.string.recipe_added_to_plan, recipe.title)).emitAsync()
                            AddToPlanEvent(recipe.id).emitAsync()
                            expanded = false
                        }
                    )
                }
                if (screen == ScreenState.Shop) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.remove_from_shop)) },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = null
                            )
                        },
                        onClick = EvasLaunching {
                            NotificationEvent(getString(Res.string.recipe_removed_from_shop, recipe.title)).emitAsync()
                            RemoveFromShopEvent(recipe.id).emitAsync()
                            expanded = false
                        }
                    )
                }
                if (screen == ScreenState.Plan) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.remove_from_plan)) },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = null
                            )
                        },
                        onClick = EvasLaunching {
                            NotificationEvent(getString(Res.string.recipe_removed_from_plan, recipe.title)).emitAsync()
                            RemoveFromPlanEvent(recipe.id, incrementCounter = false).emitAsync()
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.edit)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = EvasLaunching {
                        ScreenState.set(
                            ScreenState.Add(
                                target = screen,
                                editingRecipe = recipe
                            )
                        )
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.delete)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = EvasLaunching {
                        DeleteRequestEvent(recipe).emitAsync()
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.share)) },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    onClick = { shareRecipe(recipe.title, recipe.url, context); expanded = false }
                )
                recipe.url?.let { url ->
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.add_to_bring)) },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        onClick = { shareToBring(recipe.title, url, context); expanded = false }
                    )
                }
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.padding(12.dp).width(24.dp).height(24.dp))
        }
    }
}