package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Outcome
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.analytics.linkContext
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlin.time.Duration

internal class ConfirmOperation(
    confirmationContext: CheckoutConfirmationContext,
    private val durationProvider: DurationProvider,
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<PaymentSheetConfirmationError>(Operation.Confirm, report) {
    override val params: Map<String, Any?> = buildMap {
        put("selected_lpm", confirmationContext.paymentSelection.code())
        put("is_saved_payment_method", confirmationContext.paymentSelection.isSaved)
        confirmationContext.paymentSelection.linkContext()?.let { put("link_context", it) }
        confirmationContext.linkUi?.let { put("link_ui", it.code) }
        put("invocation_source", confirmationContext.invocationSource.code)
        put("active_link_session", confirmationContext.activeLinkSession)
    }

    // Confirmation outlives the instance that reported started, so timing lives in the provider.
    override fun elapsed(): Duration? = durationProvider.end(DurationProvider.Key.CheckoutConfirmation)

    override fun analyticsError(error: PaymentSheetConfirmationError): CheckoutAnalyticsEvent.Error {
        val causeError = CheckoutAnalyticsEvent.Error.from(error.cause)
        return CheckoutAnalyticsEvent.Error(
            type = error.analyticsValue ?: causeError.type,
            code = error.errorCode ?: causeError.code,
            requestId = causeError.requestId,
        )
    }

    fun succeeded(response: CheckoutSessionResponse?) {
        reportOutcome(Outcome.Succeeded, response, error = null)
    }

    fun canceled(response: CheckoutSessionResponse?) {
        reportOutcome(Outcome.Canceled, response, error = null)
    }
}
