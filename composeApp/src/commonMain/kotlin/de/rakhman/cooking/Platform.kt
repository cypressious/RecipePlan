package de.rakhman.cooking

import androidx.compose.runtime.Composable

expect abstract class PlatformContext

@Composable
expect fun getContext(): PlatformContext

expect fun openUrl(url: String, c: PlatformContext)
expect fun shareRecipe(title: String, url: String?, c: PlatformContext)
expect fun shareToBring(title: String, url: String, c: PlatformContext)