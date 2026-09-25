package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import kotlinx.coroutines.flow.StateFlow

internal interface EmbeddedSelectionHolder {
    val selection: StateFlow<PaymentSelection?>
    val savedPaymentMethodSelectionState: StateFlow<SavedPaymentMethodSelectionState>
    val temporarySelection: StateFlow<String?>
    val previousNewSelections: Bundle

    fun setSelection(updatedSelection: PaymentSelection?, isUserInput: Boolean)

    fun setTemporarySelection(code: PaymentMethodCode?)

    fun setPreviousNewSelections(bundle: Bundle)

    fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New?
}
