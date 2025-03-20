package de.rakhman.cooking

import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url));
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}