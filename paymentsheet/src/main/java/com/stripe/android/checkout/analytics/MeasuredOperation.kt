package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.TaxRegionUpdateOutcome
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Reports a Checkout operation that starts and finishes within a single call. The operation records
 * its latest response and completion params as it progresses, so its succeeded or failed event
 * reports what it reached.
 */
internal class MeasuredOperation(
    operation: Operation,
    startedParams: Map<String, Any?>,
    response: CheckoutSessionResponse?,
    private val timeSource: TimeSource,
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<Throwable>(operation, report) {
    var response: CheckoutSessionResponse? = response
        private set

    // Queued calls of the same operation overlap, so each attempt keeps its own start mark.
    private val startedAt = timeSource.markNow()

    private val recordedCompletionParams = operation.defaultCompletionParams.toMutableMap()

    override val params: Map<String, Any?> = startedParams

    override val completionParams: Map<String, Any?>
        get() = recordedCompletionParams

    fun recordResponse(response: CheckoutSessionResponse?) {
        this.response = response
    }

    fun reportAddressUpdateResult(outcome: TaxRegionUpdateOutcome): Result<CheckoutSessionResponse> {
        putCompletionParam(DID_REQUEST_TAX_UPDATE, outcome.requestAttempted)
        return outcome.resultOrThrowCancellation().onSuccess(::recordResponse)
    }

    /** Adds a param reported with the operation's succeeded or failed event. */
    fun putCompletionParam(key: String, value: Any?) {
        recordedCompletionParams[key] = value
    }

    override fun elapsed(): Duration = startedAt.elapsedNow()

    override fun analyticsError(error: Throwable): CheckoutAnalyticsEvent.Error =
        CheckoutAnalyticsEvent.Error.from(error)
}
