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
            (map["card"] as? Map<*, *>)?.let { card ->
                card["exp_month"]?.let { params["exp_month"] = it }
                card["exp_year"]?.let { params["exp_year"] = it }
                (card["networks"] as? Map<*, *>)?.let { networks ->
                    networks["preferred"]?.let { preferred -> params["preferred_network"] = preferred }
                }
            }
            ConsumerPaymentDetails.Card.getAddressFromMap(map)?.let {
                params[it.first] = it.second
            }
        }

        return params
    }
}
