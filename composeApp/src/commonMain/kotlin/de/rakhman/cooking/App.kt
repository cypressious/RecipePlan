@file:OptIn(ExperimentalMaterial3Api::class)

package de.rakhman.cooking

import MyBottomBar
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.rakhman.cooking.events.ReloadEvent
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.compose.rememberEvasCoroutineScope
import io.sellmair.evas.emit
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
//    val scope = rememberEvasCoroutineScope()
//
//    DisposableEffect(lifecycleOwner) {
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_CREATE) {
//                scope.launch {
//                    ReloadEvent.emit()
//                }
//            }
//        }
//        lifecycleOwner.lifecycle.addObserver(observer)
//        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
//    }

    MaterialTheme {
        Scaffold(
            topBar = { MyTopBar() },
            bottomBar = { MyBottomBar() },
            floatingActionButton = {
                FloatingActionButton(onClick = { }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            val screenState = ScreenState.composeValue()

            when (screenState) {
                ScreenState.Recipes -> RecipesScreen(modifier)
                ScreenState.Plan -> PlanScreen(modifier)
            }
        }

    }
}

