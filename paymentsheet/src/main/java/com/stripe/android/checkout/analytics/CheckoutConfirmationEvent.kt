package com.stripe.android.checkout.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.utils.mapOfDurationInSeconds
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.analytics.linkContext
import com.stripe.android.paymentsheet.model.isSaved
import kotlin.time.Duration

internal sealed class CheckoutConfirmationEvent : AnalyticsEvent {
    abstract val params: Map<String, Any>

    class Succeeded(
        confirmationContext: CheckoutConfirmationContext,
        confirmationRequestId: String?,
        analyticsMetadata: AnalyticsMetadata,
        duration: Duration?,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.succeeded"
        override val params: Map<String, Any> = buildMap {
            putAll(confirmationParams(confirmationContext, confirmationRequestId))
            putAll(analyticsMetadata.paramsMap)
            putAll(duration.mapOfDurationInSeconds())
        }
    }

    class Failed(
        confirmationContext: CheckoutConfirmationContext,
        confirmationRequestId: String?,
        analyticsMetadata: AnalyticsMetadata,
        duration: Duration?,
        error: PaymentSheetConfirmationError,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.failed"
        override val params: Map<String, Any> = buildMap {
            val stripeException = error.cause as? StripeException
            put("error_type", stripeException?.stripeError?.type ?: error.analyticsValue)
            error.errorCode?.let { put("error_code", it) }
            putAll(
                confirmationParams(
                    confirmationContext = confirmationContext,
                    confirmationRequestId = confirmationRequestId ?: stripeException?.requestId,
                )
            )
            putAll(analyticsMetadata.paramsMap)
            putAll(duration.mapOfDurationInSeconds())
        }
    }

    private companion object {
        fun confirmationParams(
            confirmationContext: CheckoutConfirmationContext,
            confirmationRequestId: String?,
        ): Map<String, Any> = buildMap {
            put("selected_lpm", confirmationContext.paymentSelection.code())
            put("is_saved_payment_method", confirmationContext.paymentSelection.isSaved)
            confirmationContext.paymentSelection.linkContext()?.let { put("link_context", it) }
            put("invocation_source", confirmationContext.invocationSource.code)
            put("active_link_session", confirmationContext.activeLinkSession)
            confirmationRequestId?.let { put("request_id", it) }
        }
    }
}
