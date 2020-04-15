package com.stripe.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class PaymentMethodOptionsParams(
    val type: PaymentMethod.Type
) : StripeParamsModel, Parcelable {

    @Parcelize
    data class Card(
        var cvc: String? = null
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Card) {
        override fun toParamMap(): Map<String, Any> {
            return mapOf(type.code to
                cvc?.let {
                    mapOf(PARAM_CVC to it)
                }.orEmpty()
            )
        }

        private companion object {
            private const val PARAM_CVC = "cvc"
        }
    }

    // TODO(mshafrir-stripe): extend `PaymentMethodOptionsParams` once PaymentMethod.Type is created
    @Parcelize
    data class Sofort(
        var preferredLanguage: String? = null
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return preferredLanguage?.let {
                mapOf(PARAM_PREFERRED_LANGUAGE to it)
            }.orEmpty()
        }

        private companion object {
            private const val PARAM_PREFERRED_LANGUAGE = "preferred_language"
        }
    }
}
