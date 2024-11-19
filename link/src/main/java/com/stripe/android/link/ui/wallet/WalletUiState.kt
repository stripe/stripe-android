package com.stripe.android.link.ui.wallet

import androidx.compose.runtime.Immutable
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails.Card

@Immutable
internal data class WalletUiState(
    val supportedTypes: Set<String>,
    val paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails>,
    val selectedItem: ConsumerPaymentDetails.PaymentDetails?,
    val isProcessing: Boolean,
    val isExpanded: Boolean = false
) {
    val selectedCard: Card?
        get() = selectedItem as? Card

    val showBankAccountTerms = selectedItem is ConsumerPaymentDetails.BankAccount

    val primaryButtonState: PrimaryButtonState
        get() {
            return if (isProcessing) {
                PrimaryButtonState.Processing
            } else {
                PrimaryButtonState.Enabled
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
            selectedItem = response.paymentDetails.firstOrNull { it is ConsumerPaymentDetails.BankAccount },
            isProcessing = false
        )
    }
}
