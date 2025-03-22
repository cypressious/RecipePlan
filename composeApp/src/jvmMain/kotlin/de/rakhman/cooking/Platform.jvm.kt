package de.rakhman.cooking

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.net.URI

actual abstract class PlatformContext {
    companion object : PlatformContext()
}

actual fun openUrl(url: String, c: PlatformContext) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url));
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
actual fun getContext(): PlatformContext = PlatformContext

actual fun shareRecipe(title: String, url: String?, c: PlatformContext) {
}

actual fun shareToBring(title: String, url: String, c: PlatformContext) {
}