package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the payment element's current [selection] and [temporarySelection], plus the stash of
 * previously entered new payment methods ([previousNewSelections]).
 *
 * Two implementations exist and their contracts differ in ways callers must respect:
 * - [DefaultEmbeddedSelectionHolder] is standalone, backed directly by `SavedStateHandle`.
 * - [com.stripe.android.checkout.CheckoutControllerStateHolder] projects the selection off the single
 *   `CheckoutControllerState`. Its mutators ([setSelection], [setTemporarySelection],
 *   [setPreviousNewSelections]) require a committed backing state; calls made before the element has loaded
 *   are dropped (and reported), so do not mutate selection before load.
 *
 * Durability of [previousNewSelections] across process death is implementation-defined: the
 * `CheckoutControllerState`-backed impl persists it, whereas [DefaultEmbeddedSelectionHolder] keeps it
 * in memory only.
 */
internal interface EmbeddedSelectionHolder {
    val selection: StateFlow<PaymentSelection?>
    val temporarySelection: StateFlow<String?>
    val previousNewSelections: Bundle

    fun setSelection(updatedSelection: PaymentSelection?)

    fun setTemporarySelection(code: PaymentMethodCode?)

    fun setPreviousNewSelections(bundle: Bundle)

    fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New?
}
