package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.set

val tabs = listOf(
    ScreenState.Plan to Icons.Default.DateRange,
    ScreenState.Shop to Icons.Default.ShoppingCart,
    ScreenState.Recipes to Icons.AutoMirrored.Filled.List
)

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
            for ((state, icon) in tabs) {
                NavigationBarItem(
                    selected = screenState == state,
                    onClick = EvasLaunching { ScreenState.set(state) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            modifier = Modifier.size(32.dp),
                            contentDescription = null
                        )
                    },
                    label = { Text(state.title) }
                )
            }
        }
        Button({}) { Text("Rezepte") }
    }
}
