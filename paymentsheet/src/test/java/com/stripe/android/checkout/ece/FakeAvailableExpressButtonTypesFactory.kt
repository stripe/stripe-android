@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview

internal class FakeAvailableExpressButtonTypesFactory(
    private val availableWallets: List<WalletType> = listOf(WalletType.GooglePay),
) : AvailableExpressButtonTypesFactory {

    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State
    ): List<WalletType> {
        return availableWallets
    }
}
