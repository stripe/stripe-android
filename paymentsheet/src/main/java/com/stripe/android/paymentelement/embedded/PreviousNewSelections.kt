package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType

internal fun Bundle.stashNewSelection(selection: PaymentSelection?) {
    if (selection is PaymentSelection.New) {
        putParcelable(selection.paymentMethodType, selection)
    }
}

internal fun Bundle.previousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
    @Suppress("DEPRECATION")
    return getParcelable(code) as PaymentSelection.New?
}
