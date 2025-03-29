package de.rakhman.cooking

import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import de.rakhman.cooking.backend.SheetsQuickstart
import de.rakhman.cooking.database.DriverFactory
import de.rakhman.cooking.states.launchRecipesState
import de.rakhman.cooking.ui.App
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.compose.installEvas
import kotlinx.coroutines.*
import org.jetbrains.compose.reload.DevelopmentEntryPoint

fun main() {
    System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
    val events = Events()
    val states = States()

    val scope = CoroutineScope(Dispatchers.Main + events + states)
    scope.launch {
        val driver = DriverFactory().createDriver()
        val database = Database(driver)

        launchRecipesState(database, async(Dispatchers.IO) {
            SheetsQuickstart.getSheetsService()
        })
    }

    singleWindowApplication(
        state = WindowState(width = 400.dp, height = 700.dp, position = WindowPosition(1100.dp, 50.dp)),
        title = "RecipePlan",
        alwaysOnTop = true,
    ) {
        installEvas(events, states) {
            DevelopmentEntryPoint {
                App()
            }
        }
    }
}
