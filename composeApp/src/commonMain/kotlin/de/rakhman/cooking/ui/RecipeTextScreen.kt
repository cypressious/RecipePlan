package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.ChangeScreenEvent
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.emitAsync
import org.jetbrains.compose.resources.stringResource
import recipeplan.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeTextScreen(modifier: Modifier, screenState: ScreenState.RecipeText) {
    val recipe = screenState.recipe
    val uriHandler = LocalUriHandler.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title) },
                navigationIcon = {
                    val goBack = EvasLaunching {
                        ChangeScreenEvent(ScreenState.Recipes).emitAsync()
                    }
                    IconButton(onClick = goBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = recipe.text ?: "",
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (recipe.url != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { uriHandler.openUri(recipe.url) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.open_url))
                }
            }
        }
    }
}