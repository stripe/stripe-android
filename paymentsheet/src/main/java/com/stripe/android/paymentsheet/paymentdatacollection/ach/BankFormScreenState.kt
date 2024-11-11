package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Parcelable
import com.stripe.android.core.strings.ResolvableString
import kotlinx.parcelize.Parcelize
import com.stripe.android.model.PaymentMethod as PaymentMethodModel

@Parcelize
internal data class BankFormScreenState(
    private val _isProcessing: Boolean = false,
    val linkedBankAccount: LinkedBankAccount? = null,
    val error: ResolvableString? = null,
) : Parcelable {

    val isProcessing: Boolean
        get() = _isProcessing && linkedBankAccount == null

    fun processing(): BankFormScreenState {
        return copy(_isProcessing = true)
    }

    @Parcelize
    data class LinkedBankAccount(
        val resultIdentifier: ResultIdentifier,
        val bankName: String?,
        val last4: String?,
        val intentId: String?,
        val financialConnectionsSessionId: String?,
        val mandateText: ResolvableString,
        val isVerifyingWithMicrodeposits: Boolean,
    ) : Parcelable

    sealed interface ResultIdentifier : Parcelable {
        @Parcelize
        data class Session(val id: String) : ResultIdentifier

        @Parcelize
        data class PaymentMethod(val paymentMethod: PaymentMethodModel) : ResultIdentifier
    }
}

internal fun BankFormScreenState.updateWithMandate(
    mandate: ResolvableString?,
): BankFormScreenState {
    return if (linkedBankAccount != null && mandate != null) {
        copy(linkedBankAccount = linkedBankAccount.copy(mandateText = mandate))
    } else {
        this
    }
}
