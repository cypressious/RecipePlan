package de.rakhman.cooking.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.AddToPlanEvent
import de.rakhman.cooking.events.NotificationEvent
import de.rakhman.cooking.events.RemoveFromPlanEvent
import de.rakhman.cooking.states.ID_TEMPORARY
import de.rakhman.cooking.states.RecipeDto
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
    val recipeState = RecipesState.composeValue() as? RecipesState.Success ?: return
    Box(modifier = modifier.fillMaxSize()) {
        val planRecipes =
            (if (isShop) recipeState.shop else recipeState.plan)
                .mapNotNull { recipeState.byId[it] }

        AnimatedVisibility(planRecipes.isEmpty(),
            modifier = Modifier.align(Alignment.Center)) {
            Text(
                stringResource(resource = Res.string.no_entries),
                fontSize = 20.sp,
            )
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 70.dp)) {
            items(
                count = planRecipes.size,
                key = { i -> planRecipes[i].id },
                itemContent = { i ->
                    val recipe = planRecipes[i]
                    RecipeItem(
                        recipe = recipe,
                        modifier = Modifier.animateItem(),
                        slotLeft = { PlanCheckbox(recipe, isShop) },
                        slotRight = { RecipeDropdown(recipe, if (isShop) ScreenState.Shop else ScreenState.Plan) }
                    )
                    if (i != planRecipes.lastIndex) HorizontalDivider(modifier = Modifier.animateItem())
                },
            )
        }
    }

}

@Composable
private fun RowScope.PlanCheckbox(recipe: RecipeDto, isShop: Boolean) {
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
