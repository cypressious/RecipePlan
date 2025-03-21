package de.rakhman.cooking

import android.app.PendingIntent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.*
import androidx.lifecycle.coroutineScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import de.rakhman.cooking.database.DriverFactory
import de.rakhman.cooking.states.launchRecipesState
import de.rakhman.cooking.ui.App
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.compose.installEvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class MainActivity : ComponentActivity() {
    val events = Events()
    val states = States()
    lateinit var database: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val driver = DriverFactory(this).createDriver()
        database = Database(driver)

        val requestedScopes = listOf(Scope("https://www.googleapis.com/auth/spreadsheets"))
        val authorizationRequest = AuthorizationRequest.builder().setRequestedScopes(requestedScopes).build()
        Identity.getAuthorizationClient(this)
            .authorize(authorizationRequest)
            .addOnSuccessListener(
                { authorizationResult ->
                    if (authorizationResult.hasResolution()) {
                        // Access needs to be granted by the user
                        val pendingIntent = authorizationResult.pendingIntent!!
                        try {
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                12, null, 0, 0, 0, null
                            )
                        } catch (e: SendIntentException) {
                            Log.e(javaClass.name, "Couldn't start Authorization UI: " + e.getLocalizedMessage())
                        }
                    } else {
                        launchStateHandler()
                    }
                })
            .addOnFailureListener({ e -> Log.e(javaClass.name, "Failed to authorize", e) })


        setContent {
            installEvas(events, states) {
                App()
            }
        }
    }

    private fun launchStateHandler() {
        lifecycle.coroutineScope.launch {
            withContext(Dispatchers.Main + events + states) {
                val sheetsService = Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credentials
                )
                    .setApplicationName("Recipe Plan Android")
                    .build()

                launchRecipesState(database, sheetsService)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}