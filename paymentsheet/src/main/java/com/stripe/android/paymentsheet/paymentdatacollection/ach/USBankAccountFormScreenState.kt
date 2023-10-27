package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Parcelable
import androidx.annotation.StringRes
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import kotlinx.parcelize.Parcelize

internal sealed class USBankAccountFormScreenState(
    @StringRes open val error: Int? = null
) : Parcelable {
    abstract val primaryButtonText: String
    abstract val mandateText: String?
    abstract val isProcessing: Boolean

    fun copy(
        error: Int? = this.error,
        primaryButtonText: String = this.primaryButtonText,
        mandateText: String? = this.mandateText,
        isProcessing: Boolean = this.isProcessing,
    ): USBankAccountFormScreenState {
        return when (this) {
            is BillingDetailsCollection -> {
                BillingDetailsCollection(
                    error = error,
                    primaryButtonText = primaryButtonText,
                    isProcessing = isProcessing,
                )
            }
            is MandateCollection -> {
                MandateCollection(
                    paymentAccount = paymentAccount,
                    financialConnectionsSessionId = financialConnectionsSessionId,
                    intentId = intentId,
                    isProcessing = isProcessing,
                    primaryButtonText = primaryButtonText,
                    mandateText = mandateText,
                )
            }
            is VerifyWithMicrodeposits -> {
                VerifyWithMicrodeposits(
                    paymentAccount = paymentAccount,
                    financialConnectionsSessionId = financialConnectionsSessionId,
                    intentId = intentId,
                    isProcessing = isProcessing,
                    primaryButtonText = primaryButtonText,
                    mandateText = mandateText,
                )
            }
            is SavedAccount -> {
                SavedAccount(
                    financialConnectionsSessionId = financialConnectionsSessionId,
                    intentId = intentId,
                    bankName = bankName,
                    last4 = last4,
                    isProcessing = isProcessing,
                    primaryButtonText = primaryButtonText,
                    mandateText = mandateText,
                )
            }
        }
    }

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
        override val isProcessing: Boolean,
        override val primaryButtonText: String,
        override val mandateText: String?,
    ) : USBankAccountFormScreenState()

    @Parcelize
    data class VerifyWithMicrodeposits(
        val paymentAccount: BankAccount,
        val financialConnectionsSessionId: String,
        val intentId: String?,
        override val isProcessing: Boolean,
        override val primaryButtonText: String,
        override val mandateText: String?,
    ) : USBankAccountFormScreenState()

    @Parcelize
    data class SavedAccount(
        val financialConnectionsSessionId: String?,
        val intentId: String?,
        val bankName: String,
        val last4: String?,
        override val isProcessing: Boolean,
        override val primaryButtonText: String,
        override val mandateText: String?,
    ) : USBankAccountFormScreenState()
}
