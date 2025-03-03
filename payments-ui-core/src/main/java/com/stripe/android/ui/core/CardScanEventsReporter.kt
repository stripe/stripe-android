package com.stripe.android.ui.core

interface CardScanEventsReporter {
    fun scanStarted()

    fun scanSucceeded()

    fun scanFailed(error: Throwable?)

    fun scanCancelled()
}