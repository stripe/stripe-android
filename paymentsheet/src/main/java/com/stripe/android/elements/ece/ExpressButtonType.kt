@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.elements.ece

import com.stripe.android.elements.CheckoutGooglePayConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview

internal sealed class ExpressButtonType {
    data object Link : ExpressButtonType()
    data class GooglePay(
        val googlePayConfiguration: CheckoutGooglePayConfiguration,
    ) : ExpressButtonType()
}
