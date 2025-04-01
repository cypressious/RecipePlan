package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rakhman.cooking.events.SpreadsheetIdChangedEvent
import de.rakhman.cooking.states.ScreenState
import de.rakhman.cooking.states.SettingsState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.emit
import io.sellmair.evas.set

@Composable
fun SettingsScreen(modifier: Modifier) {
    Column(modifier = modifier.padding(12.dp, 4.dp, 12.dp, 12.dp)) {
        val settings = SettingsState.composeValue()
        var spreadSheetId by remember { mutableStateOf(settings?.spreadSheetsId ?: "") }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            value = spreadSheetId,
            onValueChange = { spreadSheetId = it },
            label = { Text("Google Sheets ID") },
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            singleLine = true
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = EvasLaunching { SpreadsheetIdChangedEvent(spreadSheetId).emit(); ScreenState.set(ScreenState.Plan) }
        ) {
            Text("Speichern")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = EvasLaunching { ScreenState.set(ScreenState.Plan) }
        ) {
            Text("Schließen")
        }
    }
}
