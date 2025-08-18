@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.ui.core.cardscan

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardScanEventsReporter {
    /**
     * Card scan has started.
     */
    fun onCardScanStarted(implementation: String)

    /**
     * Card scan completed successfully.
     */
    fun onCardScanSucceeded(implementation: String)

    /**
     * Card scan failed with an error.
     */
    fun onCardScanFailed(implementation: String, error: Throwable?)

    /**
     * Card scan was cancelled by the user.
     */
    fun onCardScanCancelled(implementation: String)

    /**
     * Card scan API availability check.
     */
    fun onCardScanApiCheck(implementation: String, available: Boolean, reason: String? = null)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardScanEvent