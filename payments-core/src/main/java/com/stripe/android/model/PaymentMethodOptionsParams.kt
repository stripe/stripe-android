package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

sealed class PaymentMethodOptionsParams(
    val type: PaymentMethod.Type
) : StripeParamsModel, Parcelable {
    internal abstract fun createTypeParams(): List<Pair<String, Any?>>

    override fun toParamMap(): Map<String, Any> {
        val typeParams: Map<String, Any> = createTypeParams()
            .fold(emptyMap()) { acc, (key, value) ->
                acc.plus(
                    if (key == "setup_future_usage" && value == "") {
                        // Empty values are an attempt to unset a parameter;
                        // however, setup_future_usage cannot be unset.
                        emptyMap()
                    } else {
                        value?.let { mapOf(key to it) }.orEmpty()
                    }
                )
            }

        return when {
            typeParams.isNotEmpty() -> mapOf(type.code to typeParams)
            else -> emptyMap()
        }
    }

    @Parcelize
    data class Card
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Link(
        val setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Link) {

        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage?.code
            )
        }

        private companion object {
            private const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        }
    }

    @Parcelize
    data class Blik @JvmOverloads constructor(
        var code: String,
        var setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Blik) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CODE to code,
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage?.code
            )
        }

        internal companion object {
            const val PARAM_CODE = "code"
            const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class SepaDebit(
        var setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.SepaDebit) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage?.code
            )
        }

        internal companion object {
            const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Konbini(
        private val confirmationNumber: String,
        var setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.Konbini) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CONFIRMATION_NUMBER to confirmationNumber,
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage?.code
            )
        }

        internal companion object {
            const val PARAM_CONFIRMATION_NUMBER = "confirmation_number"
            const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        }
    }

    @Parcelize
    data class WeChatPay @JvmOverloads constructor(
        var appId: String,
        var setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.WeChatPay) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CLIENT to "android",
                PARAM_APP_ID to appId,
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage?.code
            )
        }

        internal companion object {
            const val PARAM_CLIENT = "client"
            const val PARAM_APP_ID = "app_id"
            const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class WeChatPayH5(
        var setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.WeChatPay) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CLIENT to "mobile_web",
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage?.code
            )
        }

        internal companion object {
            const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
            const val PARAM_CLIENT = "client"
        }
    }

    @Parcelize
    data class USBankAccount(
        var setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null
    ) : PaymentMethodOptionsParams(PaymentMethod.Type.USBankAccount) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage?.code
            )
        }

        internal companion object {
            const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        }
    }

    /**
     * Generic SetupFutureUsage PMO object for deferred intents.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class SetupFutureUsage(
        var paymentMethodType: PaymentMethod.Type,
        var setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage
    ) : PaymentMethodOptionsParams(paymentMethodType) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_SETUP_FUTURE_USAGE to setupFutureUsage.code
            )
        }

        internal companion object {
            const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentMethodOptionsParams.setupFutureUsage(): ConfirmPaymentIntentParams.SetupFutureUsage? {
    return when (this) {
        is PaymentMethodOptionsParams.Blik -> setupFutureUsage
        is PaymentMethodOptionsParams.Card -> setupFutureUsage
        is PaymentMethodOptionsParams.SepaDebit -> setupFutureUsage
        is PaymentMethodOptionsParams.Konbini -> setupFutureUsage
        is PaymentMethodOptionsParams.Link -> setupFutureUsage
        is PaymentMethodOptionsParams.USBankAccount -> setupFutureUsage
        is PaymentMethodOptionsParams.WeChatPay -> setupFutureUsage
        is PaymentMethodOptionsParams.WeChatPayH5 -> setupFutureUsage
        is PaymentMethodOptionsParams.SetupFutureUsage -> setupFutureUsage
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentMethodOptionsParams?.updateSetupFutureUsageWithPmoSfu(
    pmoSfu: ConfirmPaymentIntentParams.SetupFutureUsage
): PaymentMethodOptionsParams? {
    val currentSfu = this?.setupFutureUsage()
    val sfuValueToSet = if (currentSfu === ConfirmPaymentIntentParams.SetupFutureUsage.OffSession) {
        // If currentSfu is off_session, it was set through the save checkbox and should not be changed.
        currentSfu
    } else {
        pmoSfu
    }

    return when (this) {
        is PaymentMethodOptionsParams.Blik -> this.copy(setupFutureUsage = sfuValueToSet)
        is PaymentMethodOptionsParams.Card -> this.copy(setupFutureUsage = sfuValueToSet)
        is PaymentMethodOptionsParams.SepaDebit -> this.copy(setupFutureUsage = sfuValueToSet)
        is PaymentMethodOptionsParams.Konbini -> this.copy(setupFutureUsage = sfuValueToSet)
        is PaymentMethodOptionsParams.Link -> this.copy(setupFutureUsage = sfuValueToSet)
        is PaymentMethodOptionsParams.USBankAccount -> this.copy(setupFutureUsage = sfuValueToSet)
        is PaymentMethodOptionsParams.WeChatPay -> this.copy(setupFutureUsage = sfuValueToSet)
        is PaymentMethodOptionsParams.WeChatPayH5 -> this.copy(setupFutureUsage = sfuValueToSet)
        is PaymentMethodOptionsParams.SetupFutureUsage -> this.copy(setupFutureUsage = sfuValueToSet)
        null -> null
    }
}
