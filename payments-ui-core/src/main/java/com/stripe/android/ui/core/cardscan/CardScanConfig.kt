@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.ui.core.cardscan

import androidx.annotation.RestrictTo
import androidx.compose.runtime.compositionLocalOf

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CardScanConfig(
    val isStripeCardScanAllowed: Boolean = false,
    val elementsSessionId: String? = null,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalCardScanConfig = compositionLocalOf { CardScanConfig() }
