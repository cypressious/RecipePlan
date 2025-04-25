package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.composeValue
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import recipeplan.composeapp.generated.resources.*

@Composable
fun RecipesScreen(modifier: Modifier) {
    val recipeState = RecipesState.composeValue()
    Column(modifier = modifier) {
        when (recipeState) {
            is RecipesState.Success -> {
                Recipes(recipeState.recipes)
            }

            RecipesState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

enum class SortOrder(
    val comparator: Comparator<Recipe>
) {
    Name(compareBy { it.title.lowercase() }),
    Counter(compareByDescending<Recipe> { it.counter }.thenBy { it.title }),
}

@Composable
private fun Recipes(recipes: List<Recipe>) {
    var filter by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(SortOrder.Name) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp, 12.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f).padding(end = 12.dp, bottom = 8.dp),
            value = filter,
            onValueChange = { filter = it },
            label = { Text(stringResource(Res.string.search)) },
            trailingIcon = {
                if (filter.isNotEmpty()) {
                    IconButton(
                        modifier = Modifier.padding(4.dp, 0.dp),
                        onClick = { filter = "" },
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(Res.string.clear_search),
                        )
                    }
                }
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
        )

        IconButton(onClick = {
            sort = SortOrder.entries[(sort.ordinal + 1) % SortOrder.entries.size]
        }) {
            Icon(
                imageVector = vectorResource(Res.drawable.bootstrap_sort_down),
                contentDescription = stringResource(Res.string.sort),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    HorizontalDivider()

    if (recipes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.no_entries), fontSize = 20.sp)
        }
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filter, sort) { listState.scrollToItem(0) }

    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 70.dp)) {
        val filteredList = if (filter.isEmpty()) {
            recipes.sortedWith(sort.comparator)
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
                val recipe = filteredList[i]
                RecipeItem(recipe = recipe, slotRight = { RecipeDropdown(recipe, ScreenState.Recipes) })
                if (i != filteredList.lastIndex) HorizontalDivider()
            },
        )
    }
}

private fun Recipe.matchesFilter(filter: String): Boolean =
    title.contains(filter, ignoreCase = true) ||
            url?.contains(filter, ignoreCase = true) == true

