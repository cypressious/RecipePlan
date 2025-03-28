@file:OptIn(ExperimentalMaterial3Api::class)

package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.events.DeleteEvent
import de.rakhman.cooking.events.DeleteRequestEvent
import de.rakhman.cooking.events.NotificationEvent
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.compose.eventsOrThrow
import io.sellmair.evas.emitAsync
import io.sellmair.evas.set
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val screenState = ScreenState.composeValue()
        val snackbarHostState = remember { SnackbarHostState() }
        val events = eventsOrThrow()
        var recipeToBeDeleted by remember { mutableStateOf<Recipe?>(null) }

        LaunchedEffect(true) {
            launch {
                events.events(NotificationEvent::class).collect {
                    snackbarHostState.showSnackbar(it.message)
                }
            }
            launch {
                events.events(DeleteRequestEvent::class).collect {
                    recipeToBeDeleted = it.recipe
                }
            }
        }

        recipeToBeDeleted?.let {
            AlertDialog(
                onDismissRequest = { recipeToBeDeleted = null },
                text = { Text("Rezept \"${it.title}\" löschen?") },
                confirmButton = {
                    TextButton(
                        onClick = EvasLaunching {
                            NotificationEvent("\"${it.title}\" gelöscht.").emitAsync()
                            DeleteEvent(it.id).emitAsync()
                            recipeToBeDeleted = null
                        }
                    ) {
                        Text("Löschen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recipeToBeDeleted = null }) {
                        Text("Abbrechen")
                    }
                }
            )
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

