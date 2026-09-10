package com.stripe.android.paymentelement.embedded

import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.StateFlow

internal interface EmbeddedSelectionHolder {
    val selection: StateFlow<PaymentSelection?>
    val temporarySelection: StateFlow<String?>
    val previousNewSelections: PreviousNewSelections

    fun setSelection(updatedSelection: PaymentSelection?)

    fun setTemporarySelection(code: PaymentMethodCode?)

    fun setPreviousNewSelections(selections: PreviousNewSelections)

    fun clearPreviousNewSelections()

    fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New?
}
