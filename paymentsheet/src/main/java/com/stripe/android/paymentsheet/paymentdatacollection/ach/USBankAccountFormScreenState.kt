package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Parcelable
import androidx.annotation.StringRes
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import kotlinx.parcelize.Parcelize

internal sealed class USBankAccountFormScreenState(
    @StringRes open val error: Int? = null,
    open val isProcessing: Boolean = false
) : Parcelable {
    abstract val primaryButtonText: String
    abstract val mandateText: String?

    @Parcelize
    data class BillingDetailsCollection(
        @StringRes override val error: Int? = null,
        override val primaryButtonText: String,
        override val isProcessing: Boolean,
    ) : USBankAccountFormScreenState() {

        override val mandateText: String?
            get() = null
    }

    @Parcelize
    data class MandateCollection(
        val paymentAccount: FinancialConnectionsAccount,
        val financialConnectionsSessionId: String,
        val intentId: String?,
        override val primaryButtonText: String,
        override val mandateText: String?,
    ) : USBankAccountFormScreenState()

    @Parcelize
    data class VerifyWithMicrodeposits(
        val paymentAccount: BankAccount,
        val financialConnectionsSessionId: String,
        val intentId: String?,
        override val primaryButtonText: String,
        override val mandateText: String?,
    ) : USBankAccountFormScreenState()

    @Parcelize
    data class SavedAccount(
        val financialConnectionsSessionId: String?,
        val intentId: String?,
        val bankName: String,
        val last4: String?,
        override val primaryButtonText: String,
        override val mandateText: String?,
    ) : USBankAccountFormScreenState()
}
