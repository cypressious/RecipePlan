package de.rakhman.cooking

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.*
import androidx.core.net.toUri
import androidx.glance.appwidget.updateAll
import de.rakhman.cooking.ui.DarkColorScheme
import de.rakhman.cooking.ui.LightColorScheme

actual typealias PlatformContext = Context

actual fun openUrl(url: String, c: PlatformContext) {
    try {
        val browserIntent = url.toUrlIntent()
        c.startActivity(browserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun String.toUrlIntent(): Intent {
    val uri = toUri()
    val browserIntent = Intent(Intent.ACTION_VIEW, uri)
    return browserIntent
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
    c.startActivity(Intent.createChooser(i, c.getString(R.string.share_recipe)))
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

actual suspend fun updateWidget(c: PlatformContext) {
    MyAppWidget().updateAll(c)
}

@Composable
actual fun getColorScheme(): ColorScheme {
    val darkTheme = isSystemInDarkTheme()
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
}