package de.rakhman.cooking

import android.content.Context
import de.rakhman.cooking.backend.buildSheetsService
import de.rakhman.cooking.database.DriverFactory
import de.rakhman.cooking.repositories.DatabaseRepository
import de.rakhman.cooking.repositories.SheetsRepository
import de.rakhman.cooking.states.RecipeContext
import de.rakhman.cooking.states.syncWithSheets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

context(context: Context)
suspend fun syncInBackground() {
    val authz = handleAuthz().await() ?: return

    val driver = DriverFactory(context).createDriver()
    val database = Database(driver)

    val spreadSheetsId =
        database.settingsQueries.selectFirst().executeAsOneOrNull() ?: return

    withContext(Dispatchers.IO) {
        val recipeContext = RecipeContext(
            database = DatabaseRepository(database),
            sheets = SheetsRepository(buildSheetsService(authz.toCredentials()), spreadSheetsId),
            platformContext = context
        )
        with(recipeContext) {
            syncWithSheets()
        }
    }
}