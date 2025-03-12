package com.stripe.android.ui.core.cardscan.events

import androidx.annotation.RestrictTo
import androidx.compose.runtime.staticCompositionLocalOf
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.uicore.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardScanReporter {
    fun onCardScanStart(): Unit
    fun onCardScanFinish(result: CardScanSheetResult): Unit
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalCardScanReporter = staticCompositionLocalOf<CardScanReporter> {
        EmptyCardScanReporter
}

private object EmptyCardScanReporter : CardScanReporter {
    override fun onCardScanStart() {
        if (BuildConfig.DEBUG) {
            error("CardScanReporter.onCardScanStart() was not reported")
        }
    }

    override fun onCardScanFinish(result: CardScanSheetResult) {
        if (BuildConfig.DEBUG) {
            error("CardScanReporter.onCardScanStart() was not reported")
        }
    }
}
