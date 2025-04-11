package de.rakhman.cooking

import android.app.Activity
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.OAuth2CredentialsWithRefresh
import kotlinx.coroutines.CompletableDeferred
import java.util.Date

const val REQUEST_CODE_AUTHZ = 12
const val TAG = "AUTHZ"

context(context: Context)
fun handleAuthz(): CompletableDeferred<AuthorizationResult?> {
    val deferred = CompletableDeferred<AuthorizationResult?>()
    Identity.getAuthorizationClient(context)
        .authorize(getAuthzRequest())
        .addOnSuccessListener { authorizationResult ->
            if (authorizationResult.hasResolution()) {
                // Access needs to be granted by the user
                val pendingIntent = authorizationResult.pendingIntent!!
                try {
                    if (context is Activity) {
                        context.startIntentSenderForResult(
                            pendingIntent.intentSender,
                            REQUEST_CODE_AUTHZ, null, 0, 0, 0, null
                        )
                    }
                    deferred.complete(null)
                } catch (e: SendIntentException) {
                    Log.e(TAG, "Couldn't start Authorization UI: " + e.localizedMessage)
                }
            } else {
                deferred.complete(authorizationResult)
            }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Failed to authorize", e)
            deferred.completeExceptionally(e)
        }

    return deferred
}

context(context: Context)
fun AuthorizationResult.toCredentials(): HttpCredentialsAdapter {
    val credentials = OAuth2CredentialsWithRefresh
        .newBuilder()
        .setAccessToken(toAccessToken())
        .setRefreshHandler {
            Log.d("Authz", "Refreshing token")
            val task = Identity.getAuthorizationClient(context).authorize(getAuthzRequest())
            val result = Tasks.await(task)
            result.toAccessToken()
        }
        .build()
    return HttpCredentialsAdapter(credentials)
}

private fun AuthorizationResult.toAccessToken(): AccessToken {
    return AccessToken.newBuilder()
        .setTokenValue(accessToken)
        .setExpirationTime(Date(System.currentTimeMillis() + 1000 * 60 * 60))
        .build()
}

private fun getAuthzRequest(): AuthorizationRequest {
    val requestedScopes = listOf(Scope(SheetsScopes.SPREADSHEETS))
    return AuthorizationRequest.builder().setRequestedScopes(requestedScopes).build()
}
