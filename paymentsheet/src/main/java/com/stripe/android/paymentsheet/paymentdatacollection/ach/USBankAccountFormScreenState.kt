package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.annotation.StringRes
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.Address

internal sealed class USBankAccountFormScreenState(
    @StringRes open val error: Int? = null
) {
    abstract val primaryButtonText: String
    abstract val mandateText: String?

    class BillingDetailsCollection(
        @StringRes override val error: Int? = null,
        val name: String,
        val email: String?,
        val phone: String?,
        val address: Address?,
        override val primaryButtonText: String,
    ) : USBankAccountFormScreenState() {

        override val mandateText: String? = null

        override fun updateInputs(
            name: String,
            email: String?,
            phone: String?,
            address: Address?,
            saveForFutureUsage: Boolean,
        ) = BillingDetailsCollection(
            error = error,
            name = name,
            email = email,
            phone = phone,
            address = address,
            primaryButtonText = primaryButtonText,
        )
    }

    data class MandateCollection(
        val name: String,
        val email: String?,
        val phone: String?,
        val address: Address?,
        val paymentAccount: FinancialConnectionsAccount,
        val financialConnectionsSessionId: String,
        val intentId: String,
        override val primaryButtonText: String,
        override val mandateText: String?,
        val saveForFutureUsage: Boolean
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(
            name: String,
            email: String?,
            phone: String?,
            address: Address?,
            saveForFutureUsage: Boolean,
        ) = this.copy(
            name = name,
            email = email,
            phone = phone,
            address = address,
            saveForFutureUsage = saveForFutureUsage,
        )
    }

    data class VerifyWithMicrodeposits(
        val name: String,
        val email: String?,
        val phone: String?,
        val address: Address?,
        val paymentAccount: BankAccount,
        val financialConnectionsSessionId: String,
        val intentId: String,
        override val primaryButtonText: String,
        override val mandateText: String?,
        val saveForFutureUsage: Boolean
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(
            name: String,
            email: String?,
            phone: String?,
            address: Address?,
            saveForFutureUsage: Boolean,
        ) = this.copy(
            name = name,
            email = email,
            phone = phone,
            address = address,
            saveForFutureUsage = saveForFutureUsage,
        )
    }

    data class SavedAccount(
        val name: String,
        val email: String?,
        val phone: String?,
        val address: Address?,
        val financialConnectionsSessionId: String?,
        val intentId: String,
        val bankName: String,
        val last4: String?,
        override val primaryButtonText: String,
        override val mandateText: String?,
        val saveForFutureUsage: Boolean
    ) : USBankAccountFormScreenState() {
        override fun updateInputs(
            name: String,
            email: String?,
            phone: String?,
            address: Address?,
            saveForFutureUsage: Boolean,
        ) = this.copy(
            name = name,
            email = email,
            phone = phone,
            address = address,
            saveForFutureUsage = saveForFutureUsage,
        )
    }

    abstract fun updateInputs(
        name: String,
        email: String?,
        phone: String?,
        address: Address?,
        saveForFutureUsage: Boolean
    ): USBankAccountFormScreenState
}
