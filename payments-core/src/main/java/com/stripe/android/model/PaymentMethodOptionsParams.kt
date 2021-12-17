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
    data class Card internal constructor(
        var cvc: String? = null,
        var network: String? = null,
        internal var moto: Boolean? = null
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Card) {

        constructor(
            cvc: String? = null,
            network: String? = null,
        ) : this(
            cvc = cvc,
            network = network,
            moto = null
        )

        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CVC to cvc,
                PARAM_NETWORK to network,
                PARAM_MOTO to moto,
            )
        }

        private companion object {
            private const val PARAM_CVC = "cvc"
            private const val PARAM_NETWORK = "network"
            private const val PARAM_MOTO = "moto"
        }
    }

    @Parcelize
    data class Blik(
        var code: String,
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Blik) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CODE to code,
            )
        }

        internal companion object {
            const val PARAM_CODE = "code"
        }
    }

    @Parcelize
    data class WeChatPay(
        var appId: String,
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.WeChatPay) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CLIENT to "android",
                PARAM_APP_ID to appId
            )
        }

        internal companion object {
            const val PARAM_CLIENT = "client"
            const val PARAM_APP_ID = "app_id"
        }
    }
}
