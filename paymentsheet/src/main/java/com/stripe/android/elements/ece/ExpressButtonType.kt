@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.elements.ece

import com.stripe.android.checkout.CheckoutSessionPreview
import com.stripe.android.elements.CheckoutGooglePayConfiguration

internal sealed class ExpressButtonType {
    data object Link : ExpressButtonType()
    data class GooglePay(
        val googlePayConfiguration: CheckoutGooglePayConfiguration,
    ) : ExpressButtonType()
}
