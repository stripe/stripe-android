package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed interface FlowControllerPaymentOptionsResult {
    val linkAccountInfo: LinkAccountUpdate.Value?
    val paymentMethods: List<PaymentMethod>?

    data class Selected(
        val paymentSelection: PaymentSelection,
        override val linkAccountInfo: LinkAccountUpdate.Value,
        override val paymentMethods: List<PaymentMethod>?,
        val autocompleteFilledAddress: Address?,
    ) : FlowControllerPaymentOptionsResult

    data class Canceled(
        val paymentSelection: PaymentSelection?,
        override val linkAccountInfo: LinkAccountUpdate.Value?,
        override val paymentMethods: List<PaymentMethod>?,
    ) : FlowControllerPaymentOptionsResult

    companion object {
        fun from(
            result: EmbeddedActivityResult,
            initialPaymentSelection: PaymentSelection?,
        ): FlowControllerPaymentOptionsResult {
            return when (result) {
                is EmbeddedActivityResult.Complete -> {
                    val paymentMethods = result.customerState?.paymentMethods
                    result.selection?.let { selection ->
                        Selected(
                            paymentSelection = selection.withLinkDetails(
                                initialPaymentSelection = initialPaymentSelection,
                                linkAccountInfo = result.linkAccountInfo,
                            ),
                            linkAccountInfo = result.linkAccountInfo,
                            paymentMethods = paymentMethods,
                            autocompleteFilledAddress = result.autocompleteFilledAddress,
                        )
                    } ?: canceled(initialPaymentSelection, result.linkAccountInfo, paymentMethods)
                }
                is EmbeddedActivityResult.Cancelled -> canceled(
                    initialPaymentSelection = initialPaymentSelection,
                    linkAccountInfo = result.linkAccountInfo,
                    paymentMethods = result.customerState?.paymentMethods,
                )
                is EmbeddedActivityResult.Error -> Canceled(
                    paymentSelection = null,
                    linkAccountInfo = null,
                    paymentMethods = null,
                )
            }
        }

        private fun canceled(
            initialPaymentSelection: PaymentSelection?,
            linkAccountInfo: LinkAccountUpdate.Value,
            paymentMethods: List<PaymentMethod>?,
        ): Canceled {
            val restoredSelection = initialPaymentSelection
                ?.withLinkDetails(
                    initialPaymentSelection = initialPaymentSelection,
                    linkAccountInfo = linkAccountInfo,
                )
                ?.rebindSavedPaymentMethod(paymentMethods.orEmpty())

            return Canceled(
                paymentSelection = restoredSelection,
                linkAccountInfo = linkAccountInfo,
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
}
