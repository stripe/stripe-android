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
    @get:VisibleForTesting val elementsSessionConfigId: String?,
    @get:VisibleForTesting val paymentIntentCreationFlow: PaymentIntentCreationFlow?,
    @get:VisibleForTesting val paymentMethodSelectionFlow: PaymentMethodSelectionFlow?,
    private val stripeSdkVersion: String = StripeSdkVersion.VERSION_NAME,
) : StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, String> {
        return mapOf(
            "merchant_integration_source" to "elements",
            "merchant_integration_subtype" to "mobile",
            "merchant_integration_version" to "stripe-android/$stripeSdkVersion",
            "client_session_id" to AnalyticsRequestFactory.sessionId.toString(),
        ).plus(
            paymentMethodSelectionFlow?.let {
                mapOf("payment_method_selection_flow" to it.paramValue)
            }.orEmpty()
        ).plus(
            paymentIntentCreationFlow?.let {
                mapOf("payment_intent_creation_flow" to it.paramValue)
            }.orEmpty()
        ).plus(
            elementsSessionConfigId?.let {
                mapOf("elements_session_config_id" to it)
            }.orEmpty()
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
