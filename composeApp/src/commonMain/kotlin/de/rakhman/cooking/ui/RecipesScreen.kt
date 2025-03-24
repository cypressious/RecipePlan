package de.rakhman.cooking.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.*
import de.rakhman.cooking.events.AddToPlanEvent
import de.rakhman.cooking.events.AddToShopEvent
import de.rakhman.cooking.events.DeleteEvent
import de.rakhman.cooking.states.ID_TEMPORARY
import de.rakhman.cooking.states.RecipesState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.emitAsync

@Composable
fun RecipesScreen(modifier: Modifier) {
    val recipeState = RecipesState.Key.composeValue()
    Column(modifier = modifier) {
        when (recipeState) {
            is RecipesState.Success -> {
                Recipes(recipeState.recipes)
            }

            RecipesState.Loading -> {
                Box(modifier = Modifier.Companion.fillMaxSize(), contentAlignment = Alignment.Companion.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun Recipes(recipes: List<Recipe>) {
    var filter by remember { mutableStateOf("") }
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp, 12.dp, 12.dp),
        value = filter,
        onValueChange = { filter = it.trim() },
        label = { Text("Suche") },
        trailingIcon = {
            if (filter.isNotEmpty()) {
                IconButton(
                    modifier = Modifier.padding(4.dp, 0.dp),
                    onClick = { filter = "" },
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Suche leeren",
                    )
                }
            }
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
    )
    HorizontalDivider()

    val listState = rememberLazyListState()
    LaunchedEffect(filter) { listState.scrollToItem(0) }

    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 70.dp)) {
        val filteredList = if (filter.isEmpty()) {
            recipes
        } else {
            recipes
                .filter { it.matchesFilter(filter) }
                .sortedWith(compareByDescending<Recipe> {
                    it.title.startsWith(filter, ignoreCase = true)
                }.thenByDescending {
                    it.title.contains(filter, ignoreCase = true)
                })
        }

        items(
            count = filteredList.size,
            key = { i -> filteredList[i].id },
            itemContent = { i ->
                RecipeItem(recipe = filteredList[i])
                if (i != filteredList.lastIndex) Divider()
            },
        )
    }
}

private fun Recipe.matchesFilter(filter: String): Boolean =
    title.contains(filter, ignoreCase = true) ||
            url?.contains(filter, ignoreCase = true) == true

@Composable
fun RecipeItem(recipe: Recipe) {
    val context = getContext()
    Row(modifier = Modifier.clickable(onClick = {
        recipe.url?.let { openUrl(it, context) }
    })) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).weight(1f),
        ) {
            Text(text = recipe.title, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
            recipe.url?.let { Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray) }
        }

        var expanded by remember { mutableStateOf(false) }

        Box(modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp), contentAlignment = Alignment.Center) {
            if (recipe.id != ID_TEMPORARY) {
                IconButton(onClick = { expanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Optionen")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Auf die Einkaufsliste") },
                        onClick = EvasLaunching { AddToShopEvent(recipe.id).emitAsync(); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Auf den Plan") },
                        onClick = EvasLaunching { AddToPlanEvent(recipe.id, null).emitAsync(); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Bearbeiten") },
                        onClick = EvasLaunching { expanded = false } // TODO
                    )
                    DropdownMenuItem(
                        text = { Text("Löschen") },
                        onClick = EvasLaunching { DeleteEvent(recipe.id).emitAsync(); expanded = false }
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
                CircularProgressIndicator(modifier = Modifier.padding(12.dp).width(24.dp).height(24.dp))
            }
        }
    }
}