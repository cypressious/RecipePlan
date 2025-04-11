package de.rakhman.cooking

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.*
import androidx.core.net.toUri

actual typealias PlatformContext = Context

actual fun openUrl(url: String, c: PlatformContext) {
    try {
        val uri = url.toUri()
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

actual fun shareRecipe(title: String, url: String?, c: PlatformContext) {
    val i = Intent(Intent.ACTION_SEND)
    i.setType("text/plain")
    i.putExtra(Intent.EXTRA_SUBJECT, title)
    url?.let { i.putExtra(Intent.EXTRA_TEXT, it) }
    c.startActivity(Intent.createChooser(i, "Rezept teilen"))
}

actual fun shareToBring(title: String, url: String, c: PlatformContext) {
    val i = Intent(Intent.ACTION_SEND)
    i.setType("text/plain")
    i.setPackage("ch.publisheria.bring")
    i.putExtra(Intent.EXTRA_SUBJECT, title)
    i.putExtra(Intent.EXTRA_TEXT, url)
    try {
        c.startActivity(i)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}