package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.set


@Composable
fun MyBottomBar() {
    BottomAppBar {
        NavigationBar(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .height(70.dp),
        ) {
            val screenState = ScreenState.composeValue()

            NavigationBarItem(
                selected =  screenState == ScreenState.Plan,
                onClick = EvasLaunching { ScreenState.set(ScreenState.Plan) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        modifier = Modifier.size(32.dp),
                        contentDescription = null
                    )
                },
                label = { Text("Plan") }
            )
            NavigationBarItem(
                selected = screenState == ScreenState.Recipes,
                onClick = EvasLaunching { ScreenState.set(ScreenState.Recipes) },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        modifier = Modifier.size(32.dp),
                        contentDescription = null
                    )
                },
                label = { Text("Rezepte") }
            )
        }
        Button({}) { Text("Rezepte") }
    }
}
