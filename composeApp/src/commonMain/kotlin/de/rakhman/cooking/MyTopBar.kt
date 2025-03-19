package de.rakhman.cooking

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import de.rakhman.cooking.events.ReloadEvent
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.emit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar() {
    val screenState = ScreenState.composeValue()

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = { Text(screenState.title) },
        actions = {
            IconButton(onClick = EvasLaunching { ReloadEvent.emit() }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Neu laden"
                )
            }
        },
        modifier = Modifier.shadow(4.dp)
    )
}