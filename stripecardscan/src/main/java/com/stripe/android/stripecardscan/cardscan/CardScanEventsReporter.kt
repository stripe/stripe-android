package com.stripe.android.stripecardscan.cardscan

import com.stripe.android.stripecardscan.scanui.CancellationReason

internal interface CardScanEventsReporter {
    fun scanStarted()

    fun scanSucceeded()

    fun scanFailed(error: Throwable?)

    fun scanCancelled(reason: CancellationReason)

    fun scanMlKitFoundPan()

    fun scanMlKitFoundExp()

    fun scanDarkniteFoundPan()

    fun scanModelsDisagree()
}
