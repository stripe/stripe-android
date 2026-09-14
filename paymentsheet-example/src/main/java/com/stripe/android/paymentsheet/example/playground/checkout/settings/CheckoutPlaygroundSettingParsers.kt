package com.stripe.android.paymentsheet.example.playground.checkout.settings

import androidx.compose.ui.graphics.Color

internal fun decodeFloat(
    value: String,
    minimum: Float,
    minimumExclusive: Boolean,
): Result<Float> {
    val number = value.toFloatOrNull()?.takeIf(Float::isFinite)
        ?: return invalid(message = "Enter a finite number")
    if (number < minimum || minimumExclusive && number == minimum) {
        return invalid(
            message = if (minimumExclusive) "Must be greater than $minimum" else "Must be at least $minimum",
        )
    }
    return Result.success(number)
}

@Suppress("MagicNumber")
internal fun formatFloat(value: Float): String {
    return if (value % 1f == 0f) value.toInt().toString() else value.toString()
}

@Suppress("MagicNumber")
internal fun parseColor(value: String): Result<Color> {
    val hex = value.removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return invalid(message = "Use #RRGGBB or #AARRGGBB")
    val argb = (if (hex.length == 6) "FF$hex" else hex).toLongOrNull(16)
        ?: return invalid(message = "Use #RRGGBB or #AARRGGBB")
    return Result.success(Color(argb))
}

internal fun <T> invalid(message: String): Result<T> {
    return Result.failure(IllegalArgumentException(message))
}
