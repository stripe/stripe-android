package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ConsumerPaymentDetailsUpdateParams(
    val id: String,
    val isDefault: Boolean? = null,
    val cardPaymentMethodCreateParamsMap: Map<String, @RawValue Any>? = null,
    val clientAttributionMetadataParams: Map<String, String>,
) : StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, Any> {
        val params: MutableMap<String, Any> = mutableMapOf()

        // When updating a payment that is not the default and you send isDefault=false to the server,
        // you get "Can't unset payment details when it's not the default", so send nil instead of false
        if (isDefault == true) params["is_default"] = true

        cardPaymentMethodCreateParamsMap?.let { map ->
            params.addCardParams(map)
            params.addAddressParams(map)
            params.addEmailParam(map)
        }

        if (clientAttributionMetadataParams.isNotEmpty()) {
            params[PARAM_CLIENT_ATTRIBUTION_METADATA] = clientAttributionMetadataParams
        }

        return params
    }

    private fun MutableMap<String, Any>.addCardParams(map: Map<String, @RawValue Any>) {
        (map["card"] as? Map<*, *>)?.let { card ->
            card["exp_month"]?.let { this["exp_month"] = it }
            card["exp_year"]?.let { this["exp_year"] = it }
            (card["networks"] as? Map<*, *>)?.let { networks ->
                networks["preferred"]?.let { preferred -> this["preferred_network"] = preferred }
            }
        }
    }

    private fun MutableMap<String, Any>.addAddressParams(map: Map<String, @RawValue Any>) {
        getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams(map)?.let {
            this[it.first] = it.second
        }
    }

    private fun MutableMap<String, Any>.addEmailParam(map: Map<String, @RawValue Any>) {
        val billingDetails = map["billing_details"] as? Map<*, *>
        val emailAddress = billingDetails?.get("email") as? String
        emailAddress?.let { this["billing_email_address"] = it }
    }

    private companion object {
        const val PARAM_CLIENT_ATTRIBUTION_METADATA = "client_attribution_metadata"
    }
}
