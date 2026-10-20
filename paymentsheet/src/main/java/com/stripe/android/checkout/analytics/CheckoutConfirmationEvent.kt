package com.stripe.android.checkout.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.utils.mapOfDurationInSeconds
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.analytics.linkContext
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.checkoutAnalyticsParams
import kotlin.time.Duration

internal sealed class CheckoutConfirmationEvent : AnalyticsEvent {
    abstract val params: Map<String, Any>

    class Succeeded(
        confirmationContext: CheckoutConfirmationContext,
        response: CheckoutSessionResponse?,
        duration: Duration?,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.succeeded"
        override val params: Map<String, Any> = buildMap {
            putAll(confirmationParams(confirmationContext, response))
            putAll(duration.mapOfDurationInSeconds())
        }
    }

    class Failed(
        confirmationContext: CheckoutConfirmationContext,
        response: CheckoutSessionResponse?,
        duration: Duration?,
        error: PaymentSheetConfirmationError,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.failed"
        override val params: Map<String, Any> = buildMap {
            val stripeException = (error as? PaymentSheetConfirmationError.Stripe)?.cause as? StripeException
            put("error_type", stripeException?.stripeError?.type ?: error.analyticsValue)
            error.errorCode?.let { put("error_code", it) }
            putAll(confirmationParams(confirmationContext, response))
            putAll(duration.mapOfDurationInSeconds())
        }
    }

    private companion object {
        fun confirmationParams(
            confirmationContext: CheckoutConfirmationContext,
            response: CheckoutSessionResponse?,
        ): Map<String, Any> = buildMap {
            put("selected_lpm", confirmationContext.paymentSelection.code())
            put("is_saved_payment_method", confirmationContext.paymentSelection.isSaved)
            confirmationContext.paymentSelection.linkContext()?.let { put("link_context", it) }
            put("invocation_source", confirmationContext.invocationSource.code)
            put("active_link_session", confirmationContext.activeLinkSession)
            confirmationContext.checkoutConfigId?.let { put("checkout_config_id", it) }
            putAll(response?.checkoutAnalyticsParams().orEmpty())
        }
    }
}
