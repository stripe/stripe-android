package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.paymentelement.EmbeddedPaymentElement

internal object EmbeddedConfigurationFactory {
    fun create(
        merchantDisplayName: String = "Example, Inc.",
        formSheetAction: EmbeddedPaymentElement.FormSheetAction = EmbeddedPaymentElement.FormSheetAction.Confirm,
    ): EmbeddedPaymentElement.Configuration = EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
        .formSheetAction(formSheetAction)
        .build()
}
