package com.stripe.android.stripecardscan.cardscan

interface CardScanEventsReporter {
    fun scanStarted()

    fun scanSucceeded()

    fun scanFailed(error: Throwable?)

    fun scanCancelled()
}