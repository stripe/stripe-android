package com.stripe.android.checkout.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.utils.mapOfDurationInSeconds
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import kotlin.time.Duration

internal class CheckoutAnalyticsEvent(
    val operation: Operation,
    outcome: Outcome,
    analyticsMetadata: AnalyticsMetadata,
    duration: Duration?,
    error: Error?,
    operationParams: Map<String, Any?>,
) : AnalyticsEvent {
    override val eventName = "checkout_elements.${operation.code}.${outcome.code}"
    val params: Map<String, Any?> = buildMap {
        putAll(operationParams)
        putAll(analyticsMetadata.paramsMap)
        putAll(duration.mapOfDurationInSeconds())
        error?.let {
            put("error_type", it.type)
            it.code?.let { code -> put("error_code", code) }
            it.requestId?.let { requestId -> put("request_id", requestId) }
        }
    }

    enum class Operation(val code: String) {
        Confirm("confirm"),
    }

    enum class Outcome(val code: String) {
        Started("started"),
        Succeeded("succeeded"),
        Failed("failed"),
        Canceled("canceled"),
    }

    class Error(
        val type: String,
        val code: String?,
        val requestId: String?,
    ) {
        companion object {
            fun from(error: Throwable): Error {
                val stripeException = StripeException.create(error)
                return Error(
                    type = stripeException.stripeError?.type ?: stripeException.analyticsValue(),
                    code = stripeException.stripeError?.code,
                    requestId = stripeException.requestId,
                )
            }
        }
    }
}
