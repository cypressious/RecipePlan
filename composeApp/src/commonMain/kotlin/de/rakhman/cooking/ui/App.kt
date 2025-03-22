@file:OptIn(ExperimentalMaterial3Api::class)

package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.set
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        Scaffold(
            topBar = { MyTopBar() },
            bottomBar = { MyBottomBar() },
            floatingActionButton = {
                FloatingActionButton(onClick = EvasLaunching { ScreenState.set(ScreenState.Add) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            val screenState = ScreenState.composeValue()

            when (screenState) {
                ScreenState.Plan -> PlanScreen(modifier, false)
                ScreenState.Shop -> PlanScreen(modifier, true)
                ScreenState.Recipes -> RecipesScreen(modifier)
                ScreenState.Add -> AddScreen(modifier)
            }
        }

    }
}

