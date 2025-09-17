package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.google.android.gms.common.api.Api.Client
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ClientAttributionMetadataHolder {
    // TODO: any need to make this atomic?
    private var clientAttributionMetadata: ClientAttributionMetadata? = null

    fun initClientAttributionMetadata(
        elementsSessionConfigId: String? = null,
        paymentIntentCreationFlow: PaymentIntentCreationFlow? = null,
        paymentMethodSelectionFlow: PaymentMethodSelectionFlow? = null,
    ) {
        this.clientAttributionMetadata = ClientAttributionMetadata(
            elementsSessionConfigId = elementsSessionConfigId,
            paymentIntentCreationFlow = paymentIntentCreationFlow,
            paymentMethodSelectionFlow = paymentMethodSelectionFlow,
        )
    }

    fun getAndClear(): ClientAttributionMetadata? {
        val finalClientAttributionMetadata = this.clientAttributionMetadata
        this.clientAttributionMetadata = null
        return finalClientAttributionMetadata
    }
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ClientAttributionMetadata(
    var elementsSessionConfigId: String? = null,
    var paymentIntentCreationFlow: PaymentIntentCreationFlow? = null,
    var paymentMethodSelectionFlow: PaymentMethodSelectionFlow? = null,
) : StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, Any> {
        if (paymentMethodSelectionFlow == null || paymentIntentCreationFlow == null) {
            return emptyMap()
        }

        // TODO: don't use !! here.
        return mapOf("client_attribution_metadata" to mapOf(
            "elements_session_config_id" to elementsSessionConfigId.toString(),
            // TODO: does name give the right value for these?
            "payment_intent_creation_flow" to paymentIntentCreationFlow!!.lowercaseName,
            "payment_method_selection_flow" to paymentMethodSelectionFlow?.paramString.toString(),
            "merchant_integration_source" to "elements",
            "merchant_integration_subtype" to "mobile",
            "merchant_integration_source" to "stripe-android/${StripeSdkVersion.VERSION_NAME}",
            "client_session_id" to AnalyticsRequestFactory.sessionId.toString(),
        ))
    }
}

enum class PaymentIntentCreationFlow {
    Standard, Deferred;
    
    val lowercaseName: String
        get() = name.lowercase()
}

enum class PaymentMethodSelectionFlow(val paramString: String) {
    Automatic(paramString = "automatic"), MerchantSpecified(paramString = "merchant_specified");
}