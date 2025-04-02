package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.composeValue

@Composable
fun RecipesScreen(modifier: Modifier) {
    val recipeState = RecipesState.Key.composeValue()
    Column(modifier = modifier) {
        when (recipeState) {
            is RecipesState.Success -> {
                Recipes(recipeState.recipes.sortedBy { it.title.lowercase() })
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
        onValueChange = { filter = it },
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

    if (recipes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Keine Einträge", fontSize = 20.sp)
        }
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filter) { listState.scrollToItem(0) }

    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 70.dp)) {
        val filteredList = if (filter.isEmpty()) {
            recipes
        } else {
            recipes
                .filter { it.matchesFilter(filter.trim()) }
                .sortedWith(compareByDescending<Recipe> {
                    it.title.startsWith(filter.trim(), ignoreCase = true)
                }.thenByDescending {
                    it.title.contains(filter.trim(), ignoreCase = true)
                })
        }

        items(
            count = filteredList.size,
            key = { i -> filteredList[i].id },
            itemContent = { i ->
                RecipeItem(recipe = filteredList[i], ScreenState.Recipes)
                if (i != filteredList.lastIndex) Divider()
            },
        )
    }
}

private fun Recipe.matchesFilter(filter: String): Boolean =
    title.contains(filter, ignoreCase = true) ||
            url?.contains(filter, ignoreCase = true) == true

