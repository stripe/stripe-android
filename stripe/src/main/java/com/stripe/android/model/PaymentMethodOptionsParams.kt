package com.stripe.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

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
    internal data class Alipay(
        val appBundleId: String,
        val appVersionKey: String
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Alipay) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_APP_BUNDLE_ID to appBundleId,
                PARAM_APP_VERSION_KEY to appVersionKey
            )
        }

        private companion object {
            private const val PARAM_APP_BUNDLE_ID = "app_bundle_id"
            private const val PARAM_APP_VERSION_KEY = "app_version_key"
        }
    }
}
