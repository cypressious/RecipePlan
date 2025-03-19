package de.rakhman.cooking

import androidx.compose.ui.window.singleWindowApplication
import de.rakhman.cooking.states.launchDummyRecipesState
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.compose.installEvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.DevelopmentEntryPoint

fun main() {
    val driver = DriverFactory().createDriver()
    val database = Database(driver)

    val events = Events()
    val states = States()

    val scope = CoroutineScope(Dispatchers.Main + events + states)
    scope.launch {
        launchDummyRecipesState()
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
