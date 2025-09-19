package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ClientAttributionMetadata(
    private val elementsSessionConfigId: String,
    private val paymentIntentCreationFlow: PaymentIntentCreationFlow,
) : StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            "client_attribution_metadata" to mapOf(
                "elements_session_config_id" to elementsSessionConfigId,
                "payment_intent_creation_flow" to paymentIntentCreationFlow.lowercaseName,
                "merchant_integration_source" to "elements",
                "merchant_integration_subtype" to "mobile",
                "merchant_integration_source" to "stripe-android/${StripeSdkVersion.VERSION_NAME}",
                "client_session_id" to AnalyticsRequestFactory.sessionId.toString(),
            )
        )
    }

    companion object
}

enum class PaymentIntentCreationFlow {
    Standard, Deferred;

    val lowercaseName: String
        get() = name.lowercase()
}
