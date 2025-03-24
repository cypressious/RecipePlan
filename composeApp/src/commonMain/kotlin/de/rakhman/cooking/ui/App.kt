@file:OptIn(ExperimentalMaterial3Api::class)

package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import de.rakhman.cooking.events.NotificationEvent
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.compose.eventsOrThrow
import io.sellmair.evas.set
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val screenState = ScreenState.composeValue()
        val snackbarHostState = remember { SnackbarHostState() }
        val events = eventsOrThrow()

        LaunchedEffect(true) {
            events.events(NotificationEvent::class).collect {
                snackbarHostState.showSnackbar(it.message)
            }
        }

        Scaffold(
            topBar = { MyTopBar() },
            bottomBar = { MyBottomBar() },
            floatingActionButton = {
                if (screenState is ScreenState.BaseScreen) {
                    FloatingActionButton(onClick = EvasLaunching {
                        ScreenState.set(ScreenState.Add(screenState))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (screenState) {
                ScreenState.Plan -> PlanScreen(modifier, false)
                ScreenState.Shop -> PlanScreen(modifier, true)
                ScreenState.Recipes -> RecipesScreen(modifier)
                is ScreenState.Add -> AddScreen(modifier, screenState.editingRecipe, screenState.initialData)
            }
        }

    }
}

