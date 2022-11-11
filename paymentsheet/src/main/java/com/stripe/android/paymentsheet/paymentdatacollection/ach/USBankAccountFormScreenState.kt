package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.annotation.StringRes
import com.stripe.android.core.ResolvableString
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount

internal sealed class USBankAccountFormScreenState(
    @StringRes open val error: Int? = null
) {
    abstract val primaryButtonText: ResolvableString?
    abstract val mandateText: ResolvableString?

    class NameAndEmailCollection(
        @StringRes override val error: Int? = null,
        val name: String,
        val email: String?,
        override val primaryButtonText: ResolvableString?
    ) : USBankAccountFormScreenState() {

        override val mandateText: ResolvableString? = null

        override fun updateInputs(name: String, email: String?, saveForFutureUsage: Boolean) =
            NameAndEmailCollection(
                error = error,
                name = name,
                email = email,
                primaryButtonText = primaryButtonText
            )
    }

    data class MandateCollection(
        val name: String,
        val email: String?,
        val paymentAccount: FinancialConnectionsAccount,
        val financialConnectionsSessionId: String,
        val intentId: String,
        override val primaryButtonText: ResolvableString?,
        override val mandateText: ResolvableString?,
        val saveForFutureUsage: Boolean
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?, saveForFutureUsage: Boolean) =
            this.copy(name = name, email = email, saveForFutureUsage = saveForFutureUsage)
    }

    data class VerifyWithMicrodeposits(
        val name: String,
        val email: String?,
        val paymentAccount: BankAccount,
        val financialConnectionsSessionId: String,
        val intentId: String,
        override val primaryButtonText: ResolvableString?,
        override val mandateText: ResolvableString?,
        val saveForFutureUsage: Boolean
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?, saveForFutureUsage: Boolean) =
            this.copy(name = name, email = email, saveForFutureUsage = saveForFutureUsage)
    }

    data class SavedAccount(
        val name: String,
        val email: String?,
        val financialConnectionsSessionId: String?,
        val intentId: String,
        val bankName: String,
        val last4: String?,
        override val primaryButtonText: ResolvableString?,
        override val mandateText: ResolvableString?,
        val saveForFutureUsage: Boolean
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?, saveForFutureUsage: Boolean) =
            this.copy(name = name, email = email, saveForFutureUsage = saveForFutureUsage)
    }

    abstract fun updateInputs(
        name: String,
        email: String?,
        saveForFutureUsage: Boolean
    ): USBankAccountFormScreenState
}
