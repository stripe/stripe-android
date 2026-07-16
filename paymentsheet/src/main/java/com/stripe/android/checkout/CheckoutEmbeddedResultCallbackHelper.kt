package com.stripe.android.checkout

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import javax.inject.Inject

/**
 * The checkout payment element reuses the embedded `DefaultEmbeddedSheetLauncher`, which requires an
 * [EmbeddedResultCallbackHelper] to report a terminal result once a launched sheet
 * confirms. Checkout never confirms inside those sheets — its form sheet uses
 * [EmbeddedPaymentElement.FormSheetAction.Continue], so a sheet only ever returns a selection (which
 * flows back through the shared selection/customer holders) and a dismissal must not surface as a
 * checkout result. The checkout's terminal [CheckoutController.Result] is owned by its own confirm
 * flow, so this absorbs the launcher's result hook without emitting anything.
 */
internal class CheckoutEmbeddedResultCallbackHelper @Inject constructor() : EmbeddedResultCallbackHelper {
    override fun setResult(result: EmbeddedPaymentElement.Result) = Unit
}
