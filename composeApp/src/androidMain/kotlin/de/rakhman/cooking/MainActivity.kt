package de.rakhman.cooking

import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.*
import androidx.lifecycle.coroutineScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import de.rakhman.cooking.database.DriverFactory
import de.rakhman.cooking.events.ErrorEvent
import de.rakhman.cooking.states.ScreenState
import de.rakhman.cooking.states.launchRecipesState
import de.rakhman.cooking.ui.App
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.compose.installEvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_AUTHZ = 12
    }

    val events = Events()
    val states = States()
    lateinit var database: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val driver = DriverFactory(this).createDriver()
        database = Database(driver)

        val requestedScopes = listOf(Scope(SheetsScopes.SPREADSHEETS))
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
                                REQUEST_CODE_AUTHZ, null, 0, 0, 0, null
                            )
                        } catch (e: SendIntentException) {
                            Log.e(javaClass.name, "Couldn't start Authorization UI: " + e.getLocalizedMessage())
                        }
                    } else {
                        val credential = GoogleCredential().setAccessToken(authorizationResult.accessToken)
                        launchStateHandler(credential)
                    }
                })
            .addOnFailureListener({ e -> Log.e(javaClass.name, "Failed to authorize", e) })


        setContent {
            installEvas(events, states) {
                App()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_AUTHZ) {
            val authorizationResult = Identity
                .getAuthorizationClient(this)
                .getAuthorizationResultFromIntent(data)
            val credential =
                GoogleCredential().setAccessToken(authorizationResult.accessToken)
            launchStateHandler(credential)
        }
    }

    private fun launchStateHandler(credentials: Credential) {
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

                collectEventsAsync<ErrorEvent> {
                    Toast.makeText(this@MainActivity, it.e.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        val state = states.getState(ScreenState).value
        if (state is ScreenState.Add) {
            states.setState(ScreenState, state.target)
            return
        }
        super.onBackPressed()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}