package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class ConsumerPaymentDetailsUpdateParams : StripeParamsModel, Parcelable {
    abstract val id: String

    @Parcelize
    data class Card(
        override val id: String,
        val isDefault: Boolean? = null,
        val cardPaymentMethodCreateParams: PaymentMethodCreateParams? = null
    ) : ConsumerPaymentDetailsUpdateParams() {
        override fun toParamMap(): Map<String, Any> {
            val params: MutableMap<String, Any> = mutableMapOf()

            isDefault?.let { params["is_default"] = it }

            cardPaymentMethodCreateParams?.toParamMap()?.let { map ->
                (map["card"] as? Map<*, *>)?.let { card ->
                    card["exp_month"]?.let { params["exp_month"] = it }
                    card["exp_year"]?.let { params["exp_year"] = it }
                }
                ConsumerPaymentDetails.Card.getAddressFromMap(map)?.let {
                    params[it.first] = it.second
                }
            }

            return params
        }
    }
}
