@file:OptIn(ExperimentalMaterial3Api::class)

package de.rakhman.cooking.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.backhandler.*
import de.rakhman.cooking.events.DeleteEvent
import de.rakhman.cooking.events.DeleteRequestEvent
import de.rakhman.cooking.events.NotificationEvent
import de.rakhman.cooking.getColorScheme
import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.compose.installEvas
import io.sellmair.evas.emitAsync
import io.sellmair.evas.set
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import recipeplan.composeapp.generated.resources.*

@Composable
@Preview
fun AppPreview() {
    installEvas(Events(), States()) {
        App()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    MaterialTheme(getColorScheme()) {
        val screenState = ScreenState.composeValue()

        BackHandler(true, EvasLaunching {
            when (screenState) {
                is ScreenState.Add -> ScreenState.set(screenState.target)
                else -> ScreenState.set(ScreenState.Plan)
            }
        })

        val snackbarHostState = remember { SnackbarHostState() }
        var recipeToBeDeleted by remember { mutableStateOf<RecipeDto?>(null) }

        collectEventsComposable<NotificationEvent> {
            snackbarHostState.showSnackbar(it.message)
        }
        collectEventsComposable<DeleteRequestEvent> {
            recipeToBeDeleted = it.recipe
        }

        recipeToBeDeleted?.let {
            AlertDialog(
                onDismissRequest = { recipeToBeDeleted = null },
                text = { Text(stringResource(Res.string.confirm_delete_recipe, it.title)) },
                confirmButton = {
                    TextButton(
                        onClick = EvasLaunching {
                            NotificationEvent(getString(Res.string.recipe_deleted, it.title)).emitAsync()
                            DeleteEvent(it.id).emitAsync()
                            recipeToBeDeleted = null
                        }
                    ) {
                        Text(stringResource(Res.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recipeToBeDeleted = null }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }

        Scaffold(
            topBar = { MyTopBar() },
            bottomBar = { if (screenState != ScreenState.Settings) MyBottomBar() },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = screenState is ScreenState.BaseScreen,
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    FloatingActionButton(onClick = EvasLaunching {
                        ScreenState.set(ScreenState.Add(screenState as ScreenState.BaseScreen))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_recipe))
                    }
                }
            },
            snackbarHost = { SwipeableSnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.imePadding()
        ) { innerPadding ->
            AnimatedContent(screenState) {
                val modifier = Modifier.padding(innerPadding)
                when (it) {
                    ScreenState.Plan -> PlanScreen(modifier, false)
                    ScreenState.Shop -> PlanScreen(modifier, true)
                    ScreenState.Recipes -> RecipesScreen(modifier)
                    is ScreenState.Add -> AddScreen(modifier, it.editingRecipe, it.initialData)
                    ScreenState.Settings -> SettingsScreen(modifier)
                    ScreenState.Inspiration -> InspirationScreen(modifier)
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        SwipeToDismissBox(
            state = rememberSwipeToDismissBoxState(),
            backgroundContent = {},
            content = { Snackbar(snackbarData = data) },
            onDismiss = { data.dismiss() }
        )
    }
}

