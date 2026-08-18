@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.elements.PaymentElement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds row-selection behavior for the lifetime of a checkout controller component.
 *
 * This is intentionally not persisted: the host supplies it again when recreating its presenter.
 */
@Singleton
internal class CheckoutRowSelectionBehaviorHolder @Inject constructor() {
    var behavior: PaymentElement.RowSelectionBehavior = PaymentElement.RowSelectionBehavior.default()
}
