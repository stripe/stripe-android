package com.stripe.android.stripecardscan.cardscan

import com.stripe.android.stripecardscan.scanui.CancellationReason

internal interface CardScanEventsReporter {
    fun scanStarted()

    fun scanSucceeded(analyticsData: CardScanAnalyticsData? = null)

    fun scanFailed(error: Throwable?, analyticsData: CardScanAnalyticsData? = null)

    fun scanCancelled(reason: CancellationReason, analyticsData: CardScanAnalyticsData? = null)
}
