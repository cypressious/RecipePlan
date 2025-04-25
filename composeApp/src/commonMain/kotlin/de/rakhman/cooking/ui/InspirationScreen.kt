package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.composeValue

@Composable
fun InspirationScreen(modifier: Modifier) {
    val state = RecipesState.composeValue() as? RecipesState.Success ?: return
    var windowed by remember { mutableStateOf(state.recipes.shuffled().windowed(3, 3)) }

    Column(modifier.fillMaxSize().padding(12.dp)) {
        Text("Hello ${windowed.size}") // comment here

        if (windowed.isEmpty()) {
            // TODO
        } else {
            val recipes = windowed.first()

            recipes.forEach {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    RecipeItem(it, ScreenState.Inspiration)
                }
            }

            Spacer(Modifier.weight(1f))

            FilledTonalButton(
                onClick = { windowed = windowed.drop(1) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Next")
            }
        }
    }
}
