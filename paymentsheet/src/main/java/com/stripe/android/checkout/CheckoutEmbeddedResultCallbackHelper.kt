@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementScope
import javax.inject.Inject

/**
 * Bridges the reused embedded sheet-launcher machinery back to the checkout API: it maps the
 * [EmbeddedPaymentElement.Result] produced by the launched sheet onto a [CheckoutController.Result]
 * and forwards it to the merchant's [CheckoutController.ResultCallback].
 */
@EmbeddedPaymentElementScope
internal class CheckoutEmbeddedResultCallbackHelper @Inject constructor(
    private val resultCallback: CheckoutController.ResultCallback,
) : EmbeddedResultCallbackHelper {
    override fun setResult(result: EmbeddedPaymentElement.Result) {
        resultCallback.onResult(result.toCheckoutResult())
    }
}

private fun EmbeddedPaymentElement.Result.toCheckoutResult(): CheckoutController.Result {
    return when (this) {
        is EmbeddedPaymentElement.Result.Completed -> CheckoutController.Result.Completed()
        is EmbeddedPaymentElement.Result.Canceled -> CheckoutController.Result.Canceled()
        is EmbeddedPaymentElement.Result.Failed -> CheckoutController.Result.Failed(error)
    }
}
