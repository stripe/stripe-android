package com.stripe.android.connect.example.util

internal fun Int.toHexColor(): String = String.format("%08X", this)

internal fun String.parseHexColor(): Int? {
    if (isEmpty()) return null
    return try {
        val hex = if (startsWith("#")) this else "#$this"
        android.graphics.Color.parseColor(hex)
    } catch (_: IllegalArgumentException) {
        null
    }
}
