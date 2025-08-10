package de.rakhman.cooking.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.AddToPlanEvent
import de.rakhman.cooking.events.AddToShopEvent
import de.rakhman.cooking.events.TransferAllToPlanEvent
import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.RecipesState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.emitAsync
import org.jetbrains.compose.resources.stringResource
import recipeplan.composeapp.generated.resources.*

private const val INSPIRATION_COUNT = 3

@Composable
fun InspirationScreen(modifier: Modifier) {
    val state = RecipesState.composeValue() as? RecipesState.Success ?: return
    var windowed by remember {
        mutableStateOf(
            state.recipes
                .filter { it.id !in state.shop && it.id !in state.plan }
                .shuffled()
                .windowed(INSPIRATION_COUNT, INSPIRATION_COUNT)
        )
    }
    var checked by remember { mutableStateOf(List(INSPIRATION_COUNT) { false }) }

    Box(modifier.fillMaxSize()) {
        if (windowed.isEmpty()) {
            Text(stringResource(Res.string.no_entries))
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp).padding(bottom = 100.dp)) {
                windowed.first().forEachIndexed { index, recipe ->
                    InspirationCard(
                        recipe = recipe,
                        isChecked = checked[index],
                        onChecked = {
                            checked = checked.mapIndexed { i, it -> if (i == index) true else it }
                        },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = EvasLaunching {
                        TransferAllToPlanEvent.emitAsync()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.transfer_all_to_plan))
                }
                
                ExtendedFloatingActionButton(
                    onClick = {
                        windowed = windowed.drop(1)
                        checked = List(INSPIRATION_COUNT) { false }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(Res.string.next)) },
                    text = { Text(stringResource(Res.string.next)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InspirationCard(
    recipe: RecipeDto,
    isChecked: Boolean,
    onChecked: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors()
            .copy(if (isChecked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
    ) {
        RecipeItem(recipe, slotRight = {
            AnimatedVisibility(visible = isChecked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(Res.string.added),
                    modifier = Modifier.padding(end = 12.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        })

        HorizontalDivider()

        Row(modifier = Modifier.defaultMinSize()) {
            IconButton(EvasLaunching {
                AddToPlanEvent(recipe.id).emitAsync()
                onChecked()
            }, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(Res.string.add_to_plan),
                )
            }

            IconButton(EvasLaunching {
                AddToShopEvent(recipe.id).emitAsync()
                onChecked()
            }, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = stringResource(Res.string.add_to_shop),
                )
            }
        }
    }
}
