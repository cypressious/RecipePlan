package de.rakhman.cooking

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform