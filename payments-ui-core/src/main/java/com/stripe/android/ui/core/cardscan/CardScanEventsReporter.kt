package com.stripe.android.ui.core.cardscan

import androidx.annotation.RestrictTo
import androidx.compose.runtime.compositionLocalOf

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardScanEventsReporter {
    fun scanStarted(implementation: String)

    fun scanSucceeded(implementation: String)

    fun scanFailed(implementation: String, error: Throwable?)

    fun scanCancelled(implementation: String, reason: CancellationReason)

    fun apiCheck(implementation: String, available: Boolean, reason: String? = null)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardScanEventReporterWrapper(
    private val onCardScanStarted: (String) -> Unit,
    private val onCardScanSucceeded: (String) -> Unit,
    private val onCardScanFailed: (String, Throwable?) -> Unit,
    private val onCardScanCancelled: (String, CancellationReason) -> Unit,
    private val onCardScanApiCheck: (String, Boolean, String?) -> Unit
) : CardScanEventsReporter {
    override fun scanStarted(implementation: String) = onCardScanStarted(implementation)
    override fun scanSucceeded(implementation: String) = onCardScanSucceeded(implementation)
    override fun scanFailed(implementation: String, error: Throwable?) = onCardScanFailed(implementation, error)
    override fun scanCancelled(implementation: String, reason: CancellationReason)
        = onCardScanCancelled(implementation, reason)
    override fun apiCheck(implementation: String, available: Boolean, reason: String?)
        = onCardScanApiCheck(implementation, available, reason)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalCardScanEventsReporter = compositionLocalOf<CardScanEventsReporter> {
    error("CardScanEventsReporter not provided")
}
