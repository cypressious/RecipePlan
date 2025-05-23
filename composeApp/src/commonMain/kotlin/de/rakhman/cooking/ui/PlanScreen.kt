package de.rakhman.cooking.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.Recipe
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
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import recipeplan.composeapp.generated.resources.*
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlanScreen(modifier: Modifier, isShop: Boolean) {
    val recipeState = RecipesState.composeValue()
    Column(modifier = modifier) {
        when (recipeState) {
            is RecipesState.Success -> {
                val planRecipes =
                    (if (isShop) recipeState.shop else recipeState.plan).mapNotNull { recipeState.byId[it] }

                if (planRecipes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.no_entries), fontSize = 20.sp)
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
                                RecipeItem(
                                    recipe = recipe,
                                    slotLeft = { PlanCheckbox(recipe, isShop) },
                                    slotRight = { RecipeDropdown(recipe, if (isShop) ScreenState.Shop else ScreenState.Plan) }
                                )
                            }
                            if (i != planRecipes.lastIndex) HorizontalDivider()
                        },
                    )
                }
            }

            RecipesState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun RowScope.PlanCheckbox(recipe: Recipe, isShop: Boolean) {
    var checked by remember { mutableStateOf(false) }
    val enabled = !checked && recipe.id != ID_TEMPORARY

    Box(
        modifier = Modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                indication = ripple(bounded = false, radius = 20.dp),
                interactionSource = remember { MutableInteractionSource() },
                onValueChange = EvasLaunching<Boolean> {
                    checked = true
                    delay(250.milliseconds)
                    if (isShop) {
                        AddToPlanEvent(recipe.id).emitAsync()
                        NotificationEvent(
                            getString(
                                Res.string.recipe_added_to_plan,
                                recipe.title
                            )
                        ).emitAsync()
                    } else {
                        RemoveFromPlanEvent(recipe.id, incrementCounter = true).emitAsync()
                        NotificationEvent(
                            getString(
                                Res.string.recipe_removed_from_plan,
                                recipe.title
                            )
                        ).emitAsync()
                    }
                })
            .fillMaxHeight()
            .padding(horizontal = 24.dp)
            .align(Alignment.CenterVertically)
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = null,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
