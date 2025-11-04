@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.ui.core.cardscan

import androidx.annotation.RestrictTo
import androidx.compose.runtime.compositionLocalOf
import com.stripe.android.ui.core.BuildConfig

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
     * Card scan API availability check succeeded.
     */
    fun onCardScanApiCheckSucceeded(implementation: String)

    /**
     * Card scan API availability check failed.
     */
    fun onCardScanApiCheckFailed(implementation: String, error: Throwable? = null)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalCardScanEventsReporter = compositionLocalOf<CardScanEventsReporter> {
    EmptyCardScanEventsReporter
}

private object EmptyCardScanEventsReporter : CardScanEventsReporter {
    override fun onCardScanStarted(implementation: String) {
        errorIfDebug("onCardScanStarted")
    }

    override fun onCardScanSucceeded(implementation: String) {
        errorIfDebug("onCardScanSucceeded")
    }

    override fun onCardScanFailed(implementation: String, error: Throwable?) {
        errorIfDebug("onCardScanFailed")
    }

    override fun onCardScanCancelled(implementation: String) {
        errorIfDebug("onCardScanCancelled")
    }

    override fun onCardScanApiCheckSucceeded(implementation: String) {
        errorIfDebug("onCardScanApiCheckSucceeded")
    }

    override fun onCardScanApiCheckFailed(implementation: String, error: Throwable?) {
        errorIfDebug("onCardScanApiCheckFailed")
    }

    private fun errorIfDebug(eventName: String) {
        if (BuildConfig.DEBUG) {
            error("CardScanEventsReporter.$eventName was not reported - CardScanEventsReporter not provided")
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardScanEvent
