package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import javax.inject.Inject

/**
 * Selects tapped rows in the Embedded Payment Element. Once a customer verified inline, tapping the Link row opens
 * the Link wallet instead.
 */
internal class EmbeddedVerticalPaymentSelectionHandler @Inject constructor(
    private val selectionHolder: EmbeddedSelectionHolder,
    private val immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    private val linkInlineAuth: EmbeddedLinkInlineAuth,
) : VerticalPaymentSelectionHandler {
    override fun select(selection: PaymentSelection, isUserInput: Boolean) {
        // Picking another row ends typing in the inline Link card.
        linkInlineAuth.endTyping()
        if (selection is PaymentSelection.Link && linkInlineAuth.onLinkRowSelected()) return
        selectionHolder.setSelection(selection)
        onSelectionComplete()
    }

    override fun onSelectionComplete() {
        immediateActionHandler.invoke()
    }
}
