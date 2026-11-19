package com.stripe.android.checkout

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSupportedBy
import javax.inject.Inject
import javax.inject.Provider

/**
 * Keeps the customer's selection across checkout reloads while it's still supported.
 *
 * Checkout only reloads with a previous selection after a server-side change (a mutation, or a refresh after
 * confirmation), never after the merchant reconfigures. A form that gains elements, such as a save checkbox
 * once the session has a customer, therefore doesn't invalidate a new payment method the customer entered.
 */
internal class CheckoutSelectionChooser @Inject constructor(
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
) : EmbeddedSelectionChooser {
    override fun choose(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection?,
        newSelection: PaymentSelection?,
        newConfiguration: CommonConfiguration,
        formSheetAction: EmbeddedPaymentElement.FormSheetAction,
    ): PaymentSelection? {
        if (
            internalRowSelectionCallback.get() != null &&
            formSheetAction == EmbeddedPaymentElement.FormSheetAction.Confirm
        ) {
            return null
        }

        return previousSelection?.takeIf { it.isSupportedBy(paymentMethodMetadata, paymentMethods) }
            ?: newSelection
    }
}
