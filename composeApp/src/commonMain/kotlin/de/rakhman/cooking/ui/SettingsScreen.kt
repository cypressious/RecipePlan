package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.CreateSpreadsheetEvent
import de.rakhman.cooking.events.SpreadsheetIdChangedEvent
import de.rakhman.cooking.getContext
import de.rakhman.cooking.openUrl
import de.rakhman.cooking.signOut
import de.rakhman.cooking.states.SavingSettingsState
import de.rakhman.cooking.states.ScreenState
import de.rakhman.cooking.states.SettingsState
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.emit
import io.sellmair.evas.emitAsync
import io.sellmair.evas.set
import org.jetbrains.compose.resources.stringResource
import recipeplan.composeapp.generated.resources.*

@Composable
fun SettingsScreen(modifier: Modifier) {
    Column(modifier = modifier.padding(12.dp, 4.dp, 12.dp, 12.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
        val settings = SettingsState.composeValue()
        val savingState = SavingSettingsState.composeValue()
        var spreadSheetId by remember { mutableStateOf(settings?.spreadSheetsId ?: "") }

        Button(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(56.dp),
            onClick = EvasLaunching { CreateSpreadsheetEvent.emit(); ScreenState.set(ScreenState.Plan) },
            enabled = spreadSheetId.isBlank() && savingState == SavingSettingsState.NotSaving
        ) {
            Text(stringResource(Res.string.create_new_sheet))
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            value = spreadSheetId,
            onValueChange = { spreadSheetId = it.trim() },
            label = { Text(stringResource(Res.string.or_enter_existing_id)) },
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            singleLine = true,
            enabled = savingState == SavingSettingsState.NotSaving
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = EvasLaunching { SpreadsheetIdChangedEvent(spreadSheetId.trim()).emit(); ScreenState.set(ScreenState.Plan) },
            enabled = spreadSheetId.isNotBlank() && savingState == SavingSettingsState.NotSaving
        ) {
            Text(stringResource(Res.string.save))
        }

        val context = getContext()

        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick =  { openUrl("https://docs.google.com/spreadsheets/d/$spreadSheetId", context) },
            enabled = spreadSheetId.isNotBlank()
        ) {
            Text(stringResource(Res.string.open_google_sheet))
        }

        Text(
            stringResource(Res.string.share_recipes_hint),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = EvasLaunching { SpreadsheetIdChangedEvent(null).emitAsync(); signOut(context) },
            colors = ButtonDefaults.filledTonalButtonColors(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.error
            ),
        ) {
            Text(stringResource(Res.string.sign_out))
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = EvasLaunching { ScreenState.set(ScreenState.Plan) },
        ) {
            Text(stringResource(Res.string.close))
        }
    }
}
