package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Parcelable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.financialconnections.model.BankAccount
import kotlinx.parcelize.Parcelize

internal sealed class USBankAccountFormScreenState(
    open val error: ResolvableString? = null,
    open val isProcessing: Boolean = false
) : Parcelable {
    abstract val primaryButtonText: ResolvableString
    abstract val mandateText: ResolvableString?

    @Parcelize
    data class BillingDetailsCollection(
        override val error: ResolvableString? = null,
        override val primaryButtonText: ResolvableString,
        override val isProcessing: Boolean,
    ) : USBankAccountFormScreenState() {

        override val mandateText: ResolvableString?
            get() = null
    }

    @Parcelize
    data class MandateCollection(
        val resultIdentifier: ResultIdentifier,
        val bankName: String?,
        val last4: String?,
        val intentId: String?,
        override val primaryButtonText: ResolvableString,
        override val mandateText: ResolvableString?,
    ) : USBankAccountFormScreenState()

    @Parcelize
    data class VerifyWithMicrodeposits(
        val paymentAccount: BankAccount,
        val financialConnectionsSessionId: String,
        val intentId: String?,
        override val primaryButtonText: ResolvableString,
        override val mandateText: ResolvableString?,
    ) : USBankAccountFormScreenState()

    @Parcelize
    data class SavedAccount(
        val financialConnectionsSessionId: String?,
        val intentId: String?,
        val bankName: String,
        val last4: String?,
        override val primaryButtonText: ResolvableString,
        override val mandateText: ResolvableString?,
    ) : USBankAccountFormScreenState()

    internal sealed interface ResultIdentifier : Parcelable {
        @Parcelize
        data class Session(val id: String) : ResultIdentifier

        @Parcelize
        data class PaymentMethod(val paymentMethod: com.stripe.android.model.PaymentMethod) : ResultIdentifier
    }
}

internal fun USBankAccountFormScreenState.updateWithMandate(
    mandate: ResolvableString?,
): USBankAccountFormScreenState {
    return when (this) {
        is USBankAccountFormScreenState.BillingDetailsCollection -> {
            this
        }
        is USBankAccountFormScreenState.MandateCollection -> {
            this.copy(mandateText = mandate)
        }
        is USBankAccountFormScreenState.SavedAccount -> {
            this.copy(mandateText = mandate)
        }
        is USBankAccountFormScreenState.VerifyWithMicrodeposits -> {
            this.copy(mandateText = mandate)
        }
    }
}
