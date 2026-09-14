package com.stripe.android.paymentsheet

import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

internal object PaymentOptionsResultFactory {

    fun createSucceeded(
        paymentSelection: PaymentSelection,
        initialPaymentSelection: PaymentSelection?,
        paymentMethods: List<PaymentMethod>,
        linkAccountInfo: LinkAccountUpdate.Value,
        autocompleteFilledAddress: Address?,
    ): PaymentOptionsActivityResult.Succeeded {
        return PaymentOptionsActivityResult.Succeeded(
            paymentSelection = paymentSelection.withLinkDetails(
                initialPaymentSelection = initialPaymentSelection,
                linkAccountInfo = linkAccountInfo,
            ),
            linkAccountInfo = linkAccountInfo,
            paymentMethods = paymentMethods,
            autocompleteFilledAddress = autocompleteFilledAddress,
        )
    }

    fun createCanceled(
        initialPaymentSelection: PaymentSelection?,
        paymentMethods: List<PaymentMethod>,
        linkAccountInfo: LinkAccountUpdate.Value,
    ): PaymentOptionsActivityResult.Canceled {
        val restoredSelection = initialPaymentSelection
            ?.withLinkDetails(
                initialPaymentSelection = initialPaymentSelection,
                linkAccountInfo = linkAccountInfo,
            )
            ?.rebindSavedPaymentMethod(paymentMethods)

        return PaymentOptionsActivityResult.Canceled(
            linkAccountInfo = linkAccountInfo,
            mostRecentError = null,
            paymentSelection = restoredSelection,
            paymentMethods = paymentMethods,
        )
    }

    /**
     * - Clears Link payment details when there is no current Link account.
     * - Restores the previously selected Link payment method when the current selection omits it.
     */
    private fun PaymentSelection.withLinkDetails(
        initialPaymentSelection: PaymentSelection?,
        linkAccountInfo: LinkAccountUpdate.Value,
    ): PaymentSelection {
        return when (this) {
            is PaymentSelection.Link -> when (linkAccountInfo.account) {
                null -> copy(selectedPayment = null)
                else -> copy(
                    selectedPayment = selectedPayment ?: (initialPaymentSelection as? PaymentSelection.Link)
                        ?.selectedPayment
                )
            }
            else -> this
        }
    }

    private fun PaymentSelection.rebindSavedPaymentMethod(
        paymentMethods: List<PaymentMethod>,
    ): PaymentSelection? {
        if (this !is PaymentSelection.Saved) {
            return this
        }

        return paymentMethods.firstOrNull { it.id == paymentMethod.id }?.let { paymentMethod ->
            copy(paymentMethod = paymentMethod)
        }
    }
}
