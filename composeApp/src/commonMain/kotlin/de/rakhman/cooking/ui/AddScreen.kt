package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import de.rakhman.cooking.Recipe
import de.rakhman.cooking.events.AddEvent
import de.rakhman.cooking.events.NotificationEvent
import de.rakhman.cooking.events.UpdateEvent
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.emitAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val urlRegex =
    Regex("""https?://(www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)""")

@Composable
fun AddScreen(modifier: Modifier, editingRecipe: Recipe?, initialData: String?) {
    Column(modifier = modifier.padding(16.dp)) {
        var title by remember { mutableStateOf(editingRecipe?.title ?: "") }
        var url by remember {
            mutableStateOf(
                editingRecipe?.url
                    ?: initialData?.let { urlRegex.find(it) }?.value
                    ?: ""
            )
        }
        LaunchedEffect(url) {
            if (title.isNotEmpty() || url.isEmpty()) return@LaunchedEffect
            withContext(Dispatchers.IO) {
                try {
                    val doc = Ksoup.parseGetRequest(url)
                    title = doc.title()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val enabled = title.isNotBlank() && (url.isBlank() || url.matches(urlRegex))
        val submit = EvasLaunching {
            if (editingRecipe != null) {
                NotificationEvent("\"${title.trim()}\" gespeichert.").emitAsync()
                UpdateEvent(editingRecipe.id, title.trim(), url).emitAsync()
            } else {
                NotificationEvent("\"${title.trim()}\" hinzugefügt.").emitAsync()
                AddEvent(title.trim(), url.ifBlank { null }).emitAsync()
            }
        }
        val focusManager = LocalFocusManager.current

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titel") },
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
            label = { Text("URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (enabled) submit() }
            ),
        )
        Button(
            onClick = submit,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        ) {
            Text(if (editingRecipe != null) "Aktualisieren" else "Hinzufügen")
        }
    }
}