package de.rakhman.cooking

import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import de.rakhman.cooking.database.DriverFactory
import de.rakhman.cooking.events.ErrorEvent
import de.rakhman.cooking.events.ReloadEvent
import de.rakhman.cooking.states.ScreenState
import de.rakhman.cooking.states.launchRecipesState
import de.rakhman.cooking.ui.App
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.compose.installEvas
import io.sellmair.evas.emit
import io.sellmair.evas.emitAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class MainActivity : ComponentActivity() {
    val events = Events()
    val states = States()
    lateinit var database: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val driver = DriverFactory(this).createDriver()
        database = Database(driver)

        handleAuthz { launchStateHandler(it) }

        setContent {
            installEvas(events, states) {
                App()
            }
        }

        intent
            ?.takeIf { it.action == ACTION_SEND }
            ?.getStringExtra(EXTRA_TEXT)?.let {
                states.setState(ScreenState, ScreenState.Add(ScreenState.Recipes, initialData = it))
                intent = null
            }
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_AUTHZ) {
            val authorizationResult = Identity
                .getAuthorizationClient(this)
                .getAuthorizationResultFromIntent(data)
            launchStateHandler(authorizationResult)
        }
    }

    private fun launchStateHandler(authorizationResult: AuthorizationResult) {
        val httpCredentialsAdapter = authorizationResult.toCredentials()

        lifecycle.coroutineScope.launch {
            withContext(Dispatchers.Main + events + states) {
                val sheetsService = Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    httpCredentialsAdapter
                )
                    .setApplicationName("Recipe Plan Android")
                    .build()

                launchRecipesState(database, sheetsService)

                collectEventsAsync<ErrorEvent> {
                    Toast.makeText(this@MainActivity, it.e.toString(), Toast.LENGTH_LONG).show()
                }

                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        delay(30.seconds)
                        Log.d("MainActivity", "Reloading")
                        ReloadEvent.emit()
                    }
                }
            }
        }
    }


    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
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