package com.stripe.android.stripecardscan.framework.ml.ssd

import com.stripe.android.stripecardscan.framework.util.updateEach
import kotlin.math.exp

internal typealias ClassifierScores = FloatArray

/**
 * Compute softmax for the given row. This will replace each row value with a value normalized by
 * the sum of all the values in the row.
 */
internal fun ClassifierScores.softMax() {
    val maximum = maxOrNull() ?: return
    val rowSumExp = this.fold(0F) { acc, element -> acc + exp(element - maximum) }
    this.updateEach { exp(it - maximum) / rowSumExp }
}
