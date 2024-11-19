package com.stripe.android.paymentelement.embedded

import android.content.Context
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.darkThemeIconUrl
import com.stripe.android.paymentsheet.model.drawableResourceId
import com.stripe.android.paymentsheet.model.label
import com.stripe.android.paymentsheet.model.lightThemeIconUrl
import com.stripe.android.paymentsheet.model.paymentMethodType
import javax.inject.Inject

@ExperimentalEmbeddedPaymentElementApi
internal class PaymentOptionDisplayDataFactory @Inject constructor(
    private val iconLoader: PaymentSelection.IconLoader,
    private val context: Context,
) {
    fun create(selection: PaymentSelection?): EmbeddedPaymentElement.PaymentOptionDisplayData? {
        if (selection == null) {
            return null
        }

        return EmbeddedPaymentElement.PaymentOptionDisplayData(
            label = selection.label.resolve(context),
            imageLoader = {
                iconLoader.load(
                    drawableResourceId = selection.drawableResourceId,
                    lightThemeIconUrl = selection.lightThemeIconUrl,
                    darkThemeIconUrl = selection.darkThemeIconUrl,
                )
            },
            billingDetails = null,
            paymentMethodType = selection.paymentMethodType,
            mandateText = null,
        )
    }
}
