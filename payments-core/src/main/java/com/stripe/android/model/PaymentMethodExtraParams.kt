package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_SET_AS_DEFAULT_PAYMENT_METHOD
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentMethodExtraParams(
    val type: PaymentMethod.Type
) : StripeParamsModel, Parcelable {
    internal abstract fun createTypeParams(): List<Pair<String, Any?>>

    final override fun toParamMap(): Map<String, Any> {
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class BacsDebit(
        var confirmed: Boolean? = null
    ) : PaymentMethodExtraParams(PaymentMethod.Type.BacsDebit) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_CONFIRMED to confirmed?.toString()
            )
        }

        internal companion object {
            const val PARAM_CONFIRMED = "confirmed"
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Card(
        val setAsDefault: Boolean? = null
    ) : PaymentMethodExtraParams(PaymentMethod.Type.Card) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_SET_AS_DEFAULT_PAYMENT_METHOD to setAsDefault?.toString()
            )
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class USBankAccount(
        val setAsDefault: Boolean? = null
    ) : PaymentMethodExtraParams(PaymentMethod.Type.USBankAccount) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                PARAM_SET_AS_DEFAULT_PAYMENT_METHOD to setAsDefault?.toString()
            )
        }
    }
}
