package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import io.sellmair.evas.set
import org.jetbrains.compose.resources.stringResource
import recipeplan.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar() {
    val screenState = ScreenState.composeValue()

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = { Text(stringResource(screenState.title)) },
        navigationIcon = {
            if (screenState !is ScreenState.BaseScreen) {
                IconButton(onClick = EvasLaunching {
                    ScreenState.set(when(screenState) {
                        is ScreenState.Add -> screenState.target
                        ScreenState.Settings -> ScreenState.Plan
                        else -> screenState
                    })
                }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            }
        },
        actions = {
            if (screenState is ScreenState.BaseScreen) {
                val syncState = SyncState.composeValue()
                if (!syncState.isSyncing) {
                    IconButton(onClick = EvasLaunching { ReloadEvent.emit() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(Res.string.reload),
                        )
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp, end = 16.dp).width(24.dp))
                }

                IconButton(onClick = EvasLaunching { ScreenState.set(ScreenState.Settings) }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(Res.string.settings),
                    )
                }
            }
        },
    )
}