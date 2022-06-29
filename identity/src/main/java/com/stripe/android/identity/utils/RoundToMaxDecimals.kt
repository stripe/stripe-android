package com.stripe.android.identity.utils

import kotlin.math.roundToInt

/**
 * Round a float to max decimals. Backend requires scores uploaded with max 2 decimals.
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
