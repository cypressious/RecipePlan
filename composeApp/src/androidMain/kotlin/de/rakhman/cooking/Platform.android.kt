package de.rakhman.cooking

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual typealias PlatformContext = Context

actual fun openUrl(url: String, c: PlatformContext) {
    try {
        val uri = Uri.parse(url)
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        c.startActivity(browserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
actual fun getContext(): PlatformContext {
    return LocalContext.current
}