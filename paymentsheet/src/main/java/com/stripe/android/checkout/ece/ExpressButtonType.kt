@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.ece

import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview

internal sealed class ExpressButtonType {
    data object Link : ExpressButtonType()
    data class GooglePay(
        val googlePayConfiguration: GooglePayConfiguration.State,
    ) : ExpressButtonType()
}
