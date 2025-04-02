package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.AddToPlanEvent
import de.rakhman.cooking.events.NotificationEvent
import de.rakhman.cooking.events.RemoveFromPlanEvent
import de.rakhman.cooking.states.ID_TEMPORARY
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.emitAsync
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlanScreen(modifier: Modifier, isShop: Boolean) {
    val recipeState = RecipesState.Key.composeValue()
    Column(modifier = modifier) {
        when (recipeState) {
            is RecipesState.Success -> {
                val planRecipes =
                    (if (isShop) recipeState.shop else recipeState.plan).mapNotNull { recipeState.byId[it] }

                if (planRecipes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Keine Einträge", fontSize = 20.sp)
                    }
                    return@Column
                }

                val timestamp = System.currentTimeMillis().toString()
                val keys = planRecipes.mapIndexed { i, it -> "$i$timestamp" }
                LazyColumn(contentPadding = PaddingValues(bottom = 70.dp)) {
                    items(
                        count = planRecipes.size,
                        key = { i -> keys[i] },
                        itemContent = { i ->
                            val recipe = planRecipes[i]
                            Row {
                                var checked by remember { mutableStateOf(false) }
                                Checkbox(
                                    checked = checked,
                                    enabled = !checked && recipe.id != ID_TEMPORARY,
                                    onCheckedChange = EvasLaunching<Boolean> {
                                        checked = true
                                        delay(250.milliseconds)
                                        if (isShop) {
                                            AddToPlanEvent(recipe.id).emitAsync()
                                            NotificationEvent("\"${recipe.title}\" zum Plan hinzugefügt.").emitAsync()
                                        } else {
                                            RemoveFromPlanEvent(recipe.id).emitAsync()
                                            NotificationEvent("\"${recipe.title}\" vom Plan entfernt.").emitAsync()
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterVertically).padding(start = 12.dp),
                                )
                                RecipeItem(recipe = recipe, if (isShop) ScreenState.Shop else ScreenState.Plan)
                            }
                            if (i != planRecipes.lastIndex) Divider()
                        },
                    )
                }
            }

            RecipesState.Loading -> {
                Box(modifier = Modifier.Companion.fillMaxSize(), contentAlignment = Alignment.Companion.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}