package com.stripe.android.link.ui.wallet

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails.Card

@Immutable
internal data class WalletUiState(
    val paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails>,
    val selectedItem: ConsumerPaymentDetails.PaymentDetails?,
    val isProcessing: Boolean,
    val primaryButtonLabel: ResolvableString,
    val hasCompleted: Boolean,
) {

    val showBankAccountTerms = selectedItem is ConsumerPaymentDetails.BankAccount

    val primaryButtonState: PrimaryButtonState
        get() {
            val card = selectedItem as? Card
            val isExpired = card?.isExpired ?: false
            val requiresCvcRecollection = card?.cvcCheck?.requiresRecollection ?: false

            val disableButton = isExpired || requiresCvcRecollection

            return if (hasCompleted) {
                PrimaryButtonState.Completed
            } else if (isProcessing) {
                PrimaryButtonState.Processing
            } else if (disableButton) {
                PrimaryButtonState.Disabled
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
            selectedItem = response.paymentDetails.firstOrNull(),
            isProcessing = false
        )
    }
}
