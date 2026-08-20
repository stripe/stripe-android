package com.stripe.android.checkout

import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun PaymentElement.Configuration.LinkConfiguration.State.asPaymentSheet():
    PaymentSheet.LinkConfiguration =
    PaymentSheet.LinkConfiguration(
        display = display.asPaymentSheet(),
        collectMissingBillingDetailsForExistingPaymentMethods = true,
        allowUserEmailEdits = true,
        allowLogOut = true,
        disallowFundingSourceCreation = emptySet(),
    )

@OptIn(CheckoutSessionPreview::class)
internal fun ExpressCheckoutElement.Configuration.LinkConfiguration.State.asPaymentSheet():
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
private fun PaymentElement.Configuration.LinkConfiguration.Display.asPaymentSheet():
    PaymentSheet.LinkConfiguration.Display = when (this) {
    PaymentElement.Configuration.LinkConfiguration.Display.Automatic ->
        PaymentSheet.LinkConfiguration.Display.Automatic
    PaymentElement.Configuration.LinkConfiguration.Display.Never ->
        PaymentSheet.LinkConfiguration.Display.Never
    PaymentElement.Configuration.LinkConfiguration.Display.WalletButtonHidden ->
        PaymentSheet.LinkConfiguration.Display.WalletButtonHidden
}

@OptIn(CheckoutSessionPreview::class)
private fun ExpressCheckoutElement.Configuration.LinkConfiguration.Display.asPaymentSheet():
    PaymentSheet.LinkConfiguration.Display = when (this) {
    ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic ->
        PaymentSheet.LinkConfiguration.Display.Automatic
    ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never ->
        PaymentSheet.LinkConfiguration.Display.Never
    ExpressCheckoutElement.Configuration.LinkConfiguration.Display.WalletButtonHidden ->
        PaymentSheet.LinkConfiguration.Display.WalletButtonHidden
}
