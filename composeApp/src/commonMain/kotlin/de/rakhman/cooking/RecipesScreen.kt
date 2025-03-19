package de.rakhman.cooking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rakhman.cooking.states.RecipesState
import io.sellmair.evas.compose.composeValue

@Composable
fun RecipesScreen(modifier: Modifier) {
    val recipeState = RecipesState.Key.composeValue()
    Column(modifier = modifier) {
        when (recipeState) {
            is RecipesState.Success -> {
                val recipes = recipeState.list
                Recipes(recipes)
            }

            RecipesState.Error -> {
                Box(modifier = Modifier.Companion.fillMaxSize(), contentAlignment = Alignment.Companion.Center) {
                    Text("Fehler")
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

@Composable
private fun Recipes(recipes: List<Recipe>) {
    LazyColumn() {
        items(
            count = recipes.size,
            key = { i -> recipes[i].id },
            itemContent = { i ->
                RecipeItem(recipe = recipes[i])
                if (i != recipes.lastIndex) Divider()
            },
        )
    }
}

@Composable
fun RecipeItem(recipe: Recipe) {
    Row(modifier = Modifier.clickable(onClick = {
        openUrl(recipe.url)
    })) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).weight(1f),
        ) {
            Text(text = recipe.title, fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
            Text(text = recipe.url)
        }

        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp)) {
            IconButton(onClick = { expanded = true }) {
                Icon(imageVector =  Icons.Filled.MoreVert, contentDescription = "Optionen")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Auf den Plan") },
                    onClick = { /* Do something... */ }
                )
                DropdownMenuItem(
                    text = { Text("Bearbeiten") },
                    onClick = { /* Do something... */ }
                )
                DropdownMenuItem(
                    text = { Text("Löschen") },
                    onClick = { /* Do something... */ }
                )
            }
        }
    }
}