package com.stripe.android.link.ui.wallet

import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails.BankAccount
import com.stripe.android.model.ConsumerPaymentDetails.Card
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.uicore.forms.FormFieldEntry

internal data class WalletUiState(
    val supportedTypes: Set<String>,
    val paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails> = emptyList(),
    val selectedItem: ConsumerPaymentDetails.PaymentDetails? = null,
    val isExpanded: Boolean = false,
    val isProcessing: Boolean = false,
    val hasCompleted: Boolean = false,
    val errorMessage: ErrorMessage? = null,
    val expiryDateInput: FormFieldEntry = FormFieldEntry(value = null),
    val cvcInput: FormFieldEntry = FormFieldEntry(value = null),
    val alertMessage: ErrorMessage? = null,
    val paymentMethodIdBeingUpdated: String? = null
) {

    val selectedCard: Card?
        get() = selectedItem as? Card

    val primaryButtonState: PrimaryButtonState
        get() {
            val card = selectedItem as? Card
            val isExpired = card?.isExpired ?: false
            val requiresCvcRecollection = card?.cvcCheck?.requiresRecollection ?: false

            val isMissingExpiryDateInput = !(expiryDateInput.isComplete && cvcInput.isComplete)
            val isMissingCvcInput = !cvcInput.isComplete

            val isSelectedItemValid = selectedItem?.isValid ?: false
            val disableButton = !isSelectedItemValid ||
                (isExpired && isMissingExpiryDateInput) ||
                (requiresCvcRecollection && isMissingCvcInput)

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

    fun updateWithResponse(
        response: ConsumerPaymentDetails,
        initialSelectedItemId: String?
    ): WalletUiState {
        val selectedItem = (initialSelectedItemId ?: selectedItem?.id)?.let { itemId ->
            response.paymentDetails.firstOrNull { it.id == itemId }
        } ?: getDefaultItemSelection(response.paymentDetails)

        val isSelectedItemValid = selectedItem?.isValid ?: false

        return copy(
            paymentDetailsList = response.paymentDetails,
            selectedItem = selectedItem,
            isExpanded = if (isSelectedItemValid) isExpanded else true,
            isProcessing = false
        )
    }

    fun updateWithSetDefaultResult(
        updatedPaymentMethod: ConsumerPaymentDetails.PaymentDetails
    ): WalletUiState {
        val paymentMethods = paymentDetailsList.map { paymentMethod ->
            if (paymentMethod.id == updatedPaymentMethod.id) {
                updatedPaymentMethod
            } else {
                when (paymentMethod) {
                    is BankAccount -> paymentMethod.copy(isDefault = false)
                    is Card -> paymentMethod.copy(isDefault = false)
                }
            }
        }

        val selectedItem = paymentMethods.firstOrNull { it.id == selectedItem?.id }

        return copy(
            paymentDetailsList = paymentMethods,
            selectedItem = selectedItem,
            paymentMethodIdBeingUpdated = null
        )
    }

    fun updateWithError(errorMessage: ErrorMessage): WalletUiState {
        return copy(
            errorMessage = errorMessage,
            isProcessing = false
        )
    }

    fun updateWithPaymentResult(paymentResult: PaymentResult): WalletUiState {
        return copy(
            hasCompleted = paymentResult is PaymentResult.Completed,
            errorMessage = (paymentResult as? PaymentResult.Failed)?.throwable?.getErrorMessage(),
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

    private val ConsumerPaymentDetails.PaymentDetails.isValid: Boolean
        get() = supportedTypes.contains(type)
}
