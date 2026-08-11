package com.stripe.android.paymentsheet.model

import android.os.Bundle
import com.stripe.android.model.PaymentMethodCode

internal fun Bundle.stashNewSelection(selection: PaymentSelection?) {
    if (selection is PaymentSelection.New) {
        putParcelable(selection.paymentMethodType, selection)
    }
}

internal fun Bundle.previousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
    @Suppress("DEPRECATION")
    return getParcelable(code) as PaymentSelection.New?
}
