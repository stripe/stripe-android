package com.stripe.android.link.ui.wallet

import androidx.compose.runtime.Immutable
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.ConsumerPaymentDetails

@Immutable
internal data class WalletUiState(
    val paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails>,
    val selectedItem: ConsumerPaymentDetails.PaymentDetails?,
    val isProcessing: Boolean,
) {

    val showBankAccountTerms = selectedItem is ConsumerPaymentDetails.BankAccount

    val primaryButtonState: PrimaryButtonState
        get() {
            return if (isProcessing) {
                PrimaryButtonState.Processing
            } else {
                PrimaryButtonState.Disabled
            }
        }

    fun setProcessing(): WalletUiState {
        return copy(
            isProcessing = true,
        )
    }

    fun updateWithResponse(
        response: ConsumerPaymentDetails,
    ): WalletUiState {
        return copy(
            paymentDetailsList = response.paymentDetails,
            selectedItem = response.paymentDetails.firstOrNull(),
            isProcessing = false
        )
    }
}
