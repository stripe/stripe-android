package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ClientAttributionMetadata(
    @get:VisibleForTesting val elementsSessionConfigId: String,
    @get:VisibleForTesting val paymentIntentCreationFlow: PaymentIntentCreationFlow,
    @get:VisibleForTesting val paymentMethodSelectionFlow: PaymentMethodSelectionFlow,
) : StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            "elements_session_config_id" to elementsSessionConfigId,
            "payment_intent_creation_flow" to paymentIntentCreationFlow.paramValue,
            "payment_method_selection_flow" to paymentMethodSelectionFlow.paramValue,
            "merchant_integration_source" to "elements",
            "merchant_integration_subtype" to "mobile",
            "merchant_integration_version" to "stripe-android/${StripeSdkVersion.VERSION_NAME}",
            "client_session_id" to AnalyticsRequestFactory.sessionId.toString(),
        )
    }

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
