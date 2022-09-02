package com.stripe.android.link.ui.wallet

import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.ui.core.forms.FormFieldEntry

internal data class WalletUiState(
    val supportedTypes: Set<String>,
    val paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails> = emptyList(),
    val selectedItem: ConsumerPaymentDetails.PaymentDetails? = null,
    val isExpanded: Boolean = false,
    val isProcessing: Boolean = false,
    val hasCompleted: Boolean = false,
    val errorMessage: ErrorMessage? = null,
    val expiryDateInput: FormFieldEntry = FormFieldEntry(value = null),
    val cvcInput: FormFieldEntry = FormFieldEntry(value = null)
) {

    val primaryButtonState: PrimaryButtonState
        get() {
            val hasPaymentDetails = paymentDetailsList.isNotEmpty()
            val isExpired = (selectedItem as? ConsumerPaymentDetails.Card)?.isExpired ?: false
            val hasRequiredExpiryInput = expiryDateInput.isComplete && cvcInput.isComplete

            val disableButton = !hasPaymentDetails || (isExpired && !hasRequiredExpiryInput)

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

    val isSelectedItemValid: Boolean
        get() = selectedItem?.let { supportedTypes.contains(it.type) } ?: false

    fun updateWithResponse(
        response: ConsumerPaymentDetails,
        initialSelectedItemId: String?
    ): WalletUiState {
        // Select selectedItem if provided, otherwise the previously selected item
        val selectedItem = (initialSelectedItemId ?: selectedItem?.id)?.let { itemId ->
            response.paymentDetails.firstOrNull { it.id == itemId }
        } ?: getDefaultItemSelection(response.paymentDetails)

        return copy(
            paymentDetailsList = response.paymentDetails,
            selectedItem = selectedItem,
            isExpanded = false,
            isProcessing = false
        )
    }

    fun updateWithError(errorMessage: ErrorMessage): WalletUiState {
        return copy(errorMessage = errorMessage)
    }

    fun updateWithPaymentResult(paymentResult: PaymentResult): WalletUiState {
        return copy(
            hasCompleted = paymentResult is PaymentResult.Completed,
            isProcessing = false
        )
    }

    fun setProcessing(): WalletUiState {
        return copy(
            isProcessing = true,
            errorMessage = null
        )
    }

    /**
     * The item that should be selected by default from the [paymentDetailsList].
     *
     * @return the default item, if supported. Otherwise the first supported item on the list.
     */
    private fun getDefaultItemSelection(
        paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails>
    ) = paymentDetailsList.filter { supportedTypes.contains(it.type) }.let { filteredItems ->
        filteredItems.firstOrNull { it.isDefault } ?: filteredItems.firstOrNull()
    }
}
