package com.stripe.android.checkout

import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.ImmediateVerticalPaymentSelectionHandler
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import javax.inject.Inject

internal class CheckoutPaymentSelectionHandler @Inject constructor(
    selectionHolder: EmbeddedSelectionHolder,
    immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
) : VerticalPaymentSelectionHandler by ImmediateVerticalPaymentSelectionHandler(
    updateSelection = { selection: PaymentSelection, _ -> selectionHolder.setSelection(selection) },
    completionAction = immediateActionHandler::invoke,
)
