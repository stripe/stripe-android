package com.stripe.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class PaymentMethodOptionsParams(
    val type: PaymentMethod.Type
) : StripeParamsModel, Parcelable {
    internal abstract fun createTypeParams(): List<Pair<String, Any?>>

    override fun toParamMap(): Map<String, Any> {
        val typeParams: Map<String, Any> = createTypeParams()
            .fold(emptyMap()) { acc, (key, value) ->
                acc.plus(
                    value?.let { mapOf(key to it) }.orEmpty()
                )
            }

        return when {
            typeParams.isNotEmpty() -> mapOf(type.code to typeParams)
            else -> emptyMap()
        }
    }

    @Parcelize
    data class Card(
        var cvc: String? = null,
        var network: String? = null
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Card) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CVC to cvc,
                PARAM_NETWORK to network
            )
        }

        private companion object {
            private const val PARAM_CVC = "cvc"
            private const val PARAM_NETWORK = "network"
        }
    }

    @Parcelize
    data class Upi(
        var flow: String = "app_redirect"
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Upi) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_FLOW to flow
            )
        }

        private companion object {
            private const val PARAM_FLOW = "flow"
        }
    }
}
