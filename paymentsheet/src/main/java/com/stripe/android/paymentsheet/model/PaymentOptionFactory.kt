package com.stripe.android.paymentsheet.model

import android.content.Context
import javax.inject.Inject

internal class PaymentOptionFactory @Inject constructor(
    private val iconLoader: PaymentSelection.IconLoader,
    private val context: Context,
) {
    fun create(selection: PaymentSelection): PaymentOption {
        return PaymentOption(
            drawableResourceId = selection.drawableResourceId,
            lightThemeIconUrl = selection.lightThemeIconUrl,
            darkThemeIconUrl = selection.darkThemeIconUrl,
            label = selection.label.resolve(context),
            imageLoader = iconLoader::loadPaymentOption,
        )
    }
}
