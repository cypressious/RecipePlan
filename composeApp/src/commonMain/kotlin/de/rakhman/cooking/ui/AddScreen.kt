package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import de.rakhman.cooking.events.AddEvent
import de.rakhman.cooking.events.NotificationEvent
import de.rakhman.cooking.events.UpdateEvent
import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.emitAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import recipeplan.composeapp.generated.resources.*

private val urlRegex =
    Regex("""https?://(www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)""")

val allTargets = listOf(ScreenState.Plan, ScreenState.Shop, ScreenState.Recipes)

@Composable
fun AddScreen(modifier: Modifier, editingRecipe: RecipeDto?, initialData: String?) {
    val composeValue = ScreenState.composeValue()
    var target by remember { mutableStateOf((composeValue as? ScreenState.Add)?.target ?: ScreenState.Recipes) }
    var tags by remember { mutableStateOf(editingRecipe?.tags ?: emptySet()) }

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        var title by remember { mutableStateOf(editingRecipe?.title ?: "") }
        var url by remember {
            mutableStateOf(
                editingRecipe?.url
                    ?: initialData?.let { urlRegex.find(it) }?.value
                    ?: ""
            )
        }
        var text by remember { mutableStateOf(editingRecipe?.text ?: "") }
        
        LaunchedEffect(url) {
            if ((title.isNotEmpty() && text.isNotEmpty()) || url.isEmpty()) return@LaunchedEffect
            withContext(Dispatchers.IO) {
                try {
                    val doc = Ksoup.parseGetRequest(url)
                    if (title.isEmpty()) {
                        title = doc.title()
                    }
                    if (text.isEmpty()) {
                        // Try to extract recipe text from the page
                        val recipeText = doc.select("article, .recipe-content, .recipe-text, .recipe-instructions, .instructions, .recipe-description, .description")
                            .firstOrNull()?.text()
                        if (!recipeText.isNullOrBlank()) {
                            text = recipeText
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val enabled = title.isNotBlank() && (url.isBlank() || url.matches(urlRegex))
        val textToSave = text.ifBlank { null }
        val submit = EvasLaunching {
            if (editingRecipe != null) {
                NotificationEvent(getString(Res.string.recipe_saved, title.trim())).emitAsync()
                UpdateEvent(editingRecipe.id, title.trim(), url, tags, textToSave).emitAsync()
            } else {
                NotificationEvent(getString(Res.string.recipe_added, title.trim())).emitAsync()
                AddEvent(title.trim(), url.ifBlank { null }, tags, target, textToSave).emitAsync()
            }
        }
        val focusManager = LocalFocusManager.current

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(Res.string.title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.moveFocus(FocusDirection.Next) }
            ),
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(Res.string.url)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.moveFocus(FocusDirection.Next) }
            ),
        )
        
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(Res.string.recipe_text)) },
            singleLine = false,
            minLines = 3,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (enabled) submit() }
            ),
        )

        FlowRow(
            modifier = Modifier.padding(bottom = 14.dp),
        ) {
            val recipeState = RecipesState.composeValue()
            var allTags by remember { mutableStateOf((recipeState as? RecipesState.Success)?.allTags.orEmpty()) }

            for (tag in allTags) {
                val selected = tag in tags
                RecipeTag(tag, selected, clickable = true, onClick = {
                    if (!selected) {
                        tags += tag
                    } else {
                        tags -= tag
                    }
                })
            }

            Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.CenterStart) {
                var newTag by remember { mutableStateOf("") }
                BasicTextField(
                    value = newTag,
                    singleLine = true,
                    onValueChange = { newTag = it.trim().replace(";", "") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            tags += newTag
                            allTags += newTag
                            newTag = ""
                        }
                    ),
                )

                Text(
                    text = if (newTag.isBlank()) stringResource(Res.string.new_tag) else "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (editingRecipe == null) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                allTargets.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = allTargets.size),
                        onClick = { target = option },
                        selected = target == option,
                        label = { Text(stringResource(option.title)) }
                    )
                }
            }
        }

        Button(
            onClick = submit,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        ) {
            Text(stringResource(if (editingRecipe != null) Res.string.update else Res.string.add))
        }
    }
}
