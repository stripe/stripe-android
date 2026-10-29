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

    class Started(
        confirmationContext: CheckoutConfirmationContext,
        analyticsMetadata: AnalyticsMetadata,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.started"
        override val params: Map<String, Any> =
            confirmationParams(confirmationContext) + analyticsMetadata.paramsMap
    }

    class Succeeded(
        confirmationContext: CheckoutConfirmationContext,
        analyticsMetadata: AnalyticsMetadata,
        duration: Duration?,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.succeeded"
        override val params: Map<String, Any> = buildMap {
            putAll(confirmationParams(confirmationContext))
            putAll(analyticsMetadata.paramsMap)
            putAll(duration.mapOfDurationInSeconds())
        }
    }

    class Failed(
        confirmationContext: CheckoutConfirmationContext,
        analyticsMetadata: AnalyticsMetadata,
        duration: Duration?,
        error: PaymentSheetConfirmationError,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.failed"
        override val params: Map<String, Any> = buildMap {
            val stripeException = StripeException.create(error.cause)
            put(
                "error_type",
                error.analyticsValue ?: stripeException.stripeError?.type ?: stripeException.analyticsValue(),
            )
            (error.errorCode ?: stripeException.stripeError?.code)?.let { put("error_code", it) }
            stripeException.requestId?.let { put("request_id", it) }
            putAll(confirmationParams(confirmationContext))
            putAll(analyticsMetadata.paramsMap)
            putAll(duration.mapOfDurationInSeconds())
        }
    }

    class Canceled(
        confirmationContext: CheckoutConfirmationContext,
        analyticsMetadata: AnalyticsMetadata,
        duration: Duration?,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.canceled"
        override val params: Map<String, Any> = buildMap {
            putAll(confirmationParams(confirmationContext))
            putAll(analyticsMetadata.paramsMap)
            putAll(duration.mapOfDurationInSeconds())
        }
    }

    private companion object {
        fun confirmationParams(
            confirmationContext: CheckoutConfirmationContext,
        ): Map<String, Any> = buildMap {
            put("selected_lpm", confirmationContext.paymentSelection.code())
            put("is_saved_payment_method", confirmationContext.paymentSelection.isSaved)
            confirmationContext.paymentSelection.linkContext()?.let { put("link_context", it) }
            confirmationContext.linkUi?.let { put("link_ui", it.code) }
            put("invocation_source", confirmationContext.invocationSource.code)
            put("active_link_session", confirmationContext.activeLinkSession)
        }
    }
}
