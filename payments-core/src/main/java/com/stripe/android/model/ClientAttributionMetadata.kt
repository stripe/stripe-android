package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.networking.AnalyticsRequestFactory
import kotlinx.parcelize.Parcelize

/** Attribution shared by collection and confirmation requests, with fields scoped to each integration. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class ClientAttributionMetadata : StripeParamsModel, Parcelable {
    @get:VisibleForTesting
    abstract val elementsSessionConfigId: String?

    @get:VisibleForTesting
    abstract val paymentMethodSelectionFlow: PaymentMethodSelectionFlow?

    protected abstract val stripeSdkVersion: String

    final override fun toParamMap(): Map<String, Any> {
        val (source, subtype) = when (this) {
            is Elements -> "elements" to "mobile"
            is CheckoutSession -> "mobile-checkout" to if (isExpressCheckoutElement) {
                "mobile-express-checkout-element"
            } else {
                "mobile-payment-element"
            }
        }
        return buildMap {
            put("merchant_integration_source", source)
            put("merchant_integration_subtype", subtype)
            put("merchant_integration_version", "stripe-android/$stripeSdkVersion")
            put("client_session_id", AnalyticsRequestFactory.sessionId.toString())
            paymentMethodSelectionFlow?.let { put("payment_method_selection_flow", it.paramValue) }
            when (val metadata = this@ClientAttributionMetadata) {
                is Elements -> metadata.paymentIntentCreationFlow?.let {
                    put("payment_intent_creation_flow", it.paramValue)
                }
                is CheckoutSession -> {
                    put("checkout_session_id", metadata.checkoutSessionId)
                    metadata.checkoutConfigId?.let { put("checkout_config_id", it) }
                    put("merchant_integration_additional_elements", metadata.configuredElements)
                }
            }
            elementsSessionConfigId?.let { put("elements_session_config_id", it) }
        }
    }

    /** Elements attribution permits absent flow metadata for integrations such as CustomerSheet. */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Elements(
        override val elementsSessionConfigId: String?,
        @get:VisibleForTesting val paymentIntentCreationFlow: PaymentIntentCreationFlow?,
        override val paymentMethodSelectionFlow: PaymentMethodSelectionFlow?,
        protected override val stripeSdkVersion: String,
    ) : ClientAttributionMetadata()

    /** Checkout owns intent creation and identifies the configured session and payment surface. */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class CheckoutSession(
        override val elementsSessionConfigId: String?,
        override val paymentMethodSelectionFlow: PaymentMethodSelectionFlow?,
        val checkoutSessionId: String,
        val checkoutConfigId: String?,
        val configuredElements: List<String>,
        val isExpressCheckoutElement: Boolean,
        protected override val stripeSdkVersion: String,
    ) : ClientAttributionMetadata()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PaymentIntentCreationFlow(internal val paramValue: String) {
    Standard(paramValue = "standard"), Deferred(paramValue = "deferred")
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PaymentMethodSelectionFlow(internal val paramValue: String) {
    Automatic(paramValue = "automatic"), MerchantSpecified(paramValue = "merchant_specified")
}
