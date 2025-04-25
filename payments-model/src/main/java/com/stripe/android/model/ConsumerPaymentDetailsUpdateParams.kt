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
    val cardPaymentMethodCreateParamsMap: Map<String, @RawValue Any>? = null
) : StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, Any> {
        val params: MutableMap<String, Any> = mutableMapOf()

        isDefault?.let { params["is_default"] = it }

        cardPaymentMethodCreateParamsMap?.let { map ->
            params.addCardParams(map)
            params.addAddressParams(map)
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
        ConsumerPaymentDetails.Card.getAddressFromMap(map)?.let {
            this[it.first] = it.second
        }
    }
}
