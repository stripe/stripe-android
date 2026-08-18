package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.LinkConfiguration.State.asPaymentSheet():
    PaymentSheet.LinkConfiguration =
    PaymentSheet.LinkConfiguration(
        display = display.asPaymentSheet(),
        collectMissingBillingDetailsForExistingPaymentMethods =
            collectMissingBillingDetailsForExistingPaymentMethods,
        allowUserEmailEdits = true,
        allowLogOut = true,
        disallowFundingSourceCreation = disallowFundingSourceCreation,
    )

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutController.Configuration.LinkConfiguration.Display.asPaymentSheet():
    PaymentSheet.LinkConfiguration.Display = when (this) {
    CheckoutController.Configuration.LinkConfiguration.Display.Automatic ->
        PaymentSheet.LinkConfiguration.Display.Automatic
    CheckoutController.Configuration.LinkConfiguration.Display.Never ->
        PaymentSheet.LinkConfiguration.Display.Never
    CheckoutController.Configuration.LinkConfiguration.Display.WalletButtonHidden ->
        PaymentSheet.LinkConfiguration.Display.WalletButtonHidden
}
