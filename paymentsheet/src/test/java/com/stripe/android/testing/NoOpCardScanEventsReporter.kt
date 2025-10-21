package com.stripe.android.testing

import com.stripe.android.ui.core.cardscan.CardScanEventsReporter

/**
 * No-op implementation of [CardScanEventsReporter] for testing purposes.
 * Use this when card scan events don't need to be tracked in tests.
 */
internal object NoOpCardScanEventsReporter : CardScanEventsReporter {
    override fun onCardScanCancelled(implementation: String) = Unit
    override fun onCardScanFailed(implementation: String, error: Throwable?) = Unit
    override fun onCardScanSucceeded(implementation: String) = Unit
    override fun onCardScanStarted(implementation: String) = Unit
    override fun onCardScanApiCheckSucceeded(implementation: String) = Unit
    override fun onCardScanApiCheckFailed(implementation: String, error: Throwable?) = Unit
}
