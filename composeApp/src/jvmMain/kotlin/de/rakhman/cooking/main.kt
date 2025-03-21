package de.rakhman.cooking

import androidx.compose.ui.window.singleWindowApplication
import de.rakhman.cooking.backend.SheetsQuickstart
import de.rakhman.cooking.database.DriverFactory
import de.rakhman.cooking.states.launchRecipesState
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.compose.installEvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.reload.DevelopmentEntryPoint

fun main() {
    val events = Events()
    val states = States()

    val scope = CoroutineScope(Dispatchers.Main + events + states)
    scope.launch {
        val sheetsService = withContext(Dispatchers.IO) {
            SheetsQuickstart.getSheetsService()
        }

        val driver = DriverFactory().createDriver()
        val database = Database(driver)

        launchRecipesState(database, sheetsService)
    }

    singleWindowApplication(
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
