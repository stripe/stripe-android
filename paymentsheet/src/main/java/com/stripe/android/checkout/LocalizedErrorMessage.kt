package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.paymentelement.CheckoutSessionPreview

/**
 * Returns the message from a Checkout Session error, when available.
 *
 * Server messages are returned as received and may not be localized. When this returns `null`,
 * display a generic localized error message instead.
 */
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Throwable.localizedErrorMessage(): String? {
    return (this as? StripeException)?.stripeError?.message
}
