package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.annotation.StringRes
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed class USBankAccountFormScreenState {
    data class NameAndEmailCollection(
        @StringRes val error: Int? = null,
        val name: String,
        val email: String?,
        val primaryButtonText: String?
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?) =
            this.copy(name = name, email = email)
    }

    data class MandateCollection(
        val name: String,
        val email: String?,
        val paymentAccount: FinancialConnectionsAccount,
        val financialConnectionsSessionId: String,
        val intentId: String,
        val primaryButtonText: String?,
        val mandateText: String,
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?) =
            this.copy(name = name, email = email)
    }

    data class VerifyWithMicrodeposits(
        val name: String,
        val email: String?,
        val paymentAccount: BankAccount,
        val financialConnectionsSessionId: String,
        val intentId: String,
        val primaryButtonText: String?,
        val mandateText: String,
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?) =
            this.copy(name = name, email = email)
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
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?) =
            this.copy(name = name, email = email)
    }

    data class ConfirmIntent(
        val confirmIntentParams: ConfirmStripeIntentParams
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?) = this
    }

    data class Finished(
        val paymentSelection: PaymentSelection,
        val financialConnectionsSessionId: String,
        val intentId: String,
        val last4: String,
        val bankName: String
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(name: String, email: String?) = this
    }

    abstract fun updateInputs(name: String, email: String?): USBankAccountFormScreenState
}
