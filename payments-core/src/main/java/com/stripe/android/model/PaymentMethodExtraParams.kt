package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class LinkBankPayment(
        val bankName: String?,
        val last4: String?,
    ) : PaymentMethodExtraParams(PaymentMethod.Type.Link) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                IdentifierSpec.BankName.v1 to bankName,
                IdentifierSpec.Last4.v1 to last4,
            )
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class USBankAccount(
        val bankName: String?,
        val last4: String?,
        val usesMicrodeposits: Boolean,
    ) : PaymentMethodExtraParams(PaymentMethod.Type.USBankAccount) {
        override fun createTypeParams(): List<Pair<String, Any?>> {
            return listOf(
                IdentifierSpec.BankName.v1 to bankName,
                IdentifierSpec.Last4.v1 to last4,
                IdentifierSpec.UsesMicrodeposits.v1 to usesMicrodeposits.toString(),
            )
        }
    }
}
