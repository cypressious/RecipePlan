package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rakhman.cooking.events.AddEvent
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.emit

private val urlRegex = Regex("""https?://(www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)""")

@Composable
fun AddScreen(modifier: Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titel") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Button(
            onClick = EvasLaunching { AddEvent(title, url.ifBlank { null }).emit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && (url.isBlank() || url.matches(urlRegex))
        ) {
            Text("Hinzufügen")
        }
    }
}