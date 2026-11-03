package com.stripe.android.checkout.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.utils.mapOfDurationInSeconds
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import kotlin.time.Duration

internal class CheckoutAnalyticsEvent(
    val operation: Operation,
    outcome: Outcome?,
    analyticsMetadata: AnalyticsMetadata,
    duration: Duration?,
    error: Error?,
    operationParams: Map<String, Any?>,
) : AnalyticsEvent {
    // Single-event operations have no outcome.
    override val eventName = listOfNotNull("checkout_elements", operation.code, outcome?.code).joinToString(".")
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

    /**
     * [defaultCompletionParams] are completion params an operation reports unless it sets them, so a
     * rejected operation still reports them.
     */
    enum class Operation(
        val code: String,
        val defaultCompletionParams: Map<String, Any?> = emptyMap(),
    ) {
        Init("init"),
        Confirm("confirm"),
        ApplyPromotionCode("apply_promotion_code"),
        RemovePromotionCode("remove_promotion_code"),
        UpdateEmail("update_email"),
        UpdateShippingAddress("update_shipping_address", mapOf(DID_REQUEST_TAX_UPDATE to false)),
        UpdateBillingAddress("update_billing_address", mapOf(DID_REQUEST_TAX_UPDATE to false)),
        UpdateCurrency("update_currency"),
        RunServerUpdate("run_server_update"),
        ClearPaymentOption("clear_payment_option"),
    }

    enum class InvocationSource(val code: String) {
        Merchant("merchant"),
        PaymentElement("payment_element"),
        ExpressCheckoutElement("express_checkout_element"),
        ShippingAddressElement("shipping_address_element"),
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
