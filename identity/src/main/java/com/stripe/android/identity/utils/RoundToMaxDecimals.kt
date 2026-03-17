package com.stripe.android.identity.utils

import kotlin.math.roundToInt

internal const val UPLOAD_FLOAT_DECIMALS = 4

/**
 * Round a float to max decimals.
 *
 * e.g -
 * 3.123f.roundToMaxDecimals(2) = 3.12
 * 3.499f.roundToMaxDecimals(2) = 3.5
 */
internal fun Float.roundToMaxDecimals(decimals: Int): Float {
    var multiplier = 1.0f
    repeat(decimals) { multiplier *= 10 }
    return (this * multiplier).roundToInt() / multiplier
}
