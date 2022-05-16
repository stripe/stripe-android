package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.annotation.StringRes
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed class USBankAccountFormScreenState(
    @StringRes open val error: Int? = null
) {
    class NameAndEmailCollection(
        @StringRes override val error: Int? = null,
        val name: String,
        val email: String?,
        val primaryButtonText: String?
    ) : USBankAccountFormScreenState() {
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
        val primaryButtonText: String?,
        val mandateText: String,
        val saveForFutureUsage: Boolean,
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
        val primaryButtonText: String?,
        val mandateText: String,
        val saveForFutureUsage: Boolean,
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
        val primaryButtonText: String?,
        val mandateText: String,
        val saveForFutureUsage: Boolean,
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?, saveForFutureUsage: Boolean) =
            this.copy(name = name, email = email, saveForFutureUsage = saveForFutureUsage)
    }

    data class ConfirmIntent(
        val confirmIntentParams: ConfirmStripeIntentParams
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?, saveForFutureUsage: Boolean) = this
    }

    data class Finished(
        val paymentSelection: PaymentSelection,
        val financialConnectionsSessionId: String,
        val intentId: String,
        val last4: String,
        val bankName: String
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?, saveForFutureUsage: Boolean) = this
    }

    abstract fun updateInputs(
        name: String,
        email: String?,
        saveForFutureUsage: Boolean
    ): USBankAccountFormScreenState
}
