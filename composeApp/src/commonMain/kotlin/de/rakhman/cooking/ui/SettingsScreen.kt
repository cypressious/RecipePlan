package de.rakhman.cooking.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.SpreadsheetIdChangedEvent
import de.rakhman.cooking.states.ScreenState
import de.rakhman.cooking.states.SettingsState
import de.rakhman.cooking.*
import de.rakhman.cooking.events.CreateSpreadsheetEvent
import de.rakhman.cooking.states.SavingSettingsState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.emit
import io.sellmair.evas.set

@Composable
fun SettingsScreen(modifier: Modifier) {
    Column(modifier = modifier.padding(12.dp, 4.dp, 12.dp, 12.dp).fillMaxSize()) {
        val settings = SettingsState.composeValue()
        val savingState = SavingSettingsState.composeValue()
        var spreadSheetId by remember { mutableStateOf(settings?.spreadSheetsId ?: "") }

        Button(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(56.dp),
            onClick = EvasLaunching { CreateSpreadsheetEvent.emit(); ScreenState.set(ScreenState.Plan) },
            enabled = spreadSheetId.isBlank() && savingState == SavingSettingsState.NotSaving
        ) {
            Text("Neues Google Sheet erstellen")
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            value = spreadSheetId,
            onValueChange = { spreadSheetId = it.trim() },
            label = { Text("Oder existierende ID eingeben") },
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            singleLine = true,
            enabled = savingState == SavingSettingsState.NotSaving
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = EvasLaunching { SpreadsheetIdChangedEvent(spreadSheetId.trim()).emit(); ScreenState.set(ScreenState.Plan) },
            enabled = spreadSheetId.isNotBlank() && savingState == SavingSettingsState.NotSaving
        ) {
            Text("Speichern")
        }

        val context = getContext()

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick =  { openUrl("https://docs.google.com/spreadsheets/d/$spreadSheetId", context) },
            colors = ButtonDefaults.filledTonalButtonColors(),
            enabled = spreadSheetId.isNotBlank()
        ) {
            Text("Google Sheet öffnen")
        }

        Text(
            "Um deine Rezepte gemeinsam mit anderen Personen zu planen, gebe ihnen das Google Sheet mit Schreibrechten frei.",
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(Modifier.weight(1f))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = EvasLaunching { ScreenState.set(ScreenState.Plan) },
            colors = ButtonDefaults.elevatedButtonColors()
        ) {
            Text("Schließen")
        }
    }
}
