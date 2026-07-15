package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType

/**
 * Stashes [selection] into this bundle, keyed by its payment method type, when it is a
 * [PaymentSelection.New] — so a previously entered new payment method can be restored later. No-op for
 * a null or non-[PaymentSelection.New] selection.
 *
 * Shared by the [EmbeddedSelectionHolder] implementations so their stash/lookup logic can't drift.
 */
internal fun Bundle.stashNewSelection(selection: PaymentSelection?) {
    if (selection is PaymentSelection.New) {
        putParcelable(selection.paymentMethodType, selection)
    }
}

/** Returns the previously stashed [PaymentSelection.New] for [code], or null if none. */
internal fun Bundle.previousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
    @Suppress("DEPRECATION")
    return getParcelable(code) as PaymentSelection.New?
}
