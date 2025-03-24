package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.ReloadEvent
import de.rakhman.cooking.states.ScreenState
import de.rakhman.cooking.states.SyncState
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
            val syncState = SyncState.composeValue()
            if (!syncState.isSyncing) {
                IconButton(onClick = EvasLaunching { ReloadEvent.emit() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Neu laden",
                    )
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp, end = 16.dp).width(24.dp))
            }
        },
    )
}