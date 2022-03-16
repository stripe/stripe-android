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
        var setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
        internal var moto: Boolean? = null
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Card) {

        constructor(
            cvc: String? = null,
            network: String? = null,
            setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null
        ) : this(
            cvc = cvc,
            network = network,
            setupFutureUsage = setupFutureUsage,
            moto = null
        )

        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CVC to cvc,
                PARAM_NETWORK to network,
                PARAM_MOTO to moto,
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage?.code
            )
        }

        private companion object {
            private const val PARAM_CVC = "cvc"
            private const val PARAM_NETWORK = "network"
            private const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
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

    @Parcelize
    internal data class USBankAccount(
        var linkedAccount: LinkedAccount? = null,
        var networks: Networks? = null,
        var setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
        var verificationMethod: VerificationMethod? = null
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.USBankAccount) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_LINKED_ACCOUNT to linkedAccount,
                PARAM_NETWORKS to networks,
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage?.code,
                PARAM_VERIFICATION_METHOD to verificationMethod
            )
        }

        @Parcelize
        data class LinkedAccount(
            val permissions: List<Permission>? = null,
            val returnUrl: String? = null
        ) : Parcelable {
            enum class Permission(val value: String) {
                BALANCES("balances"),
                IDENTITY("identity"),
                PAYMENT_METHOD("payment_method"),
                TRANSACTIONS("transactions")
            }
        }

        enum class VerificationMethod(val value: String) {
            SKIP("skip"),
            AUTOMATIC("automatic"),
            INSTANT("instant"),
            MICRODEPOSITS("microdeposits"),
            INSTANT_OR_SKIP("instant_or_skip")
        }


        @Parcelize
        data class Networks(val requested: String? = null): Parcelable

        internal companion object {
            const val PARAM_LINKED_ACCOUNT = "linked_account"
            const val PARAM_NETWORKS = "networks"
            const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
            const val PARAM_VERIFICATION_METHOD = "verification_method"
        }
    }
}
