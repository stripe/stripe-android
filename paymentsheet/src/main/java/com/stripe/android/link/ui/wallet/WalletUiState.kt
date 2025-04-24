package com.stripe.android.link.ui.wallet

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails.Card
import com.stripe.android.uicore.forms.FormFieldEntry

@Immutable
internal data class WalletUiState(
    val paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails>,
    val isExpanded: Boolean = false,
    val selectedItemId: String?,
    val isProcessing: Boolean,
    val primaryButtonLabel: ResolvableString,
    val hasCompleted: Boolean,
    val canAddNewPaymentMethod: Boolean,
    val cardBeingUpdated: String? = null,
    val errorMessage: ResolvableString? = null,
    val expiryDateInput: FormFieldEntry = FormFieldEntry(null),
    val cvcInput: FormFieldEntry = FormFieldEntry(null),
    val alertMessage: ResolvableString? = null,
) {

    val selectedItem
        get() = if (selectedItemId != null) {
            paymentDetailsList.firstOrNull { it.id == selectedItemId }
        } else {
            paymentDetailsList.firstOrNull()
        }

    val selectedCard: Card? = selectedItem as? Card

    val showBankAccountTerms = selectedItem is ConsumerPaymentDetails.BankAccount

    val primaryButtonState: PrimaryButtonState
        get() {
            val card = selectedItem as? Card
            val isExpired = card?.isExpired == true
            val requiresCvcRecollection = card?.cvcCheck?.requiresRecollection ?: false

            val isMissingExpiryDateInput = (expiryDateInput.isComplete && cvcInput.isComplete).not()
            val isMissingCvcInput = cvcInput.isComplete.not()

            val disableButton = (isExpired && isMissingExpiryDateInput) ||
                (requiresCvcRecollection && isMissingCvcInput) || (cardBeingUpdated != null)

            return when {
                hasCompleted -> {
                    PrimaryButtonState.Completed
                }
                isProcessing -> {
                    PrimaryButtonState.Processing
                }
                disableButton -> {
                    PrimaryButtonState.Disabled
                }
                else -> {
                    PrimaryButtonState.Enabled
                }
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
            isProcessing = false,
            cardBeingUpdated = null
        )
    }
}
