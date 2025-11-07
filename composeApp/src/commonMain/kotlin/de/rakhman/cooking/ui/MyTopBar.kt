package de.rakhman.cooking.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.ReloadEvent
import de.rakhman.cooking.states.ScreenState
import de.rakhman.cooking.states.SyncState
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.compose.installEvas
import io.sellmair.evas.emit
import io.sellmair.evas.set
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import recipeplan.composeapp.generated.resources.*

@Composable
@Preview
fun MyTopBarPreview() {
    installEvas(Events(), States()) {
        MyTopBar()
    }
}

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
                        else -> ScreenState.Plan
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
                IconButton(onClick = EvasLaunching { if (!syncState.isSyncing) ReloadEvent.emit() }) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(Res.string.reload),
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (syncState.isSyncing) rotation else 0f
                        }
                    )
                }

                IconButton(onClick = EvasLaunching { ScreenState.set(ScreenState.Inspiration) }) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.dice_outline),
                        contentDescription = stringResource(Res.string.inspiration),
                        modifier = Modifier.padding(8.dp)
                    )
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
