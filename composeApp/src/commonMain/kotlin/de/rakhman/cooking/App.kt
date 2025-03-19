package de.rakhman.cooking


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(database: Database) {
    var recipes by remember { mutableStateOf(database.recipesQueries.selectAll().executeAsList()) }

    MaterialTheme {
        Column {
            Text("Rezepte")
            LazyColumn {
                items(
                    count = recipes.size,
                    key = { i -> recipes[i].id },
                    itemContent = { i ->
                        RecipeItem(recipe = recipes[i])
                    }
                )
            }
        }

    }
}

@Composable
fun RecipeItem(recipe: Recipe) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = recipe.title)
        Text(text = recipe.url)
    }
}