package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.RemoveFromPlanEvent
import de.rakhman.cooking.states.RecipesState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.emit

@Composable
fun PlanScreen(modifier: Modifier) {
    val recipeState = RecipesState.Key.composeValue()
    Column(modifier = modifier) {
        when (recipeState) {
            is RecipesState.Success -> {
                val byId = recipeState.recipes.associateBy { it.id }
                val planRecipes = recipeState.plan.mapNotNull { byId[it] }
                LazyColumn(contentPadding = PaddingValues(bottom = 70.dp)) {
                    items(
                        count = planRecipes.size,
                        itemContent = { i ->
                            val recipe = planRecipes[i]
                            Row {
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = EvasLaunching<Boolean> { RemoveFromPlanEvent(i).emit() },
                                    modifier = Modifier.align(Alignment.CenterVertically).padding(start = 12.dp),
                                )
                                RecipeItem(recipe = recipe)
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