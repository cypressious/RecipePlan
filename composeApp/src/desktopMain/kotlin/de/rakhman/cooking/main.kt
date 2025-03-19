package de.rakhman.cooking

import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.reload.DevelopmentEntryPoint

fun main() {
    val driver = DriverFactory().createDriver()
    val database = Database(driver)

    singleWindowApplication(
        title = "RecipePlan",
        alwaysOnTop = true
    ) {
        DevelopmentEntryPoint {
            App(database)
        }
    }
}
