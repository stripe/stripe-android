@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

class DefaultAvailableExpressButtonTypesFactoryTest {
    @Test
    fun `create keeps only wallets returned by metadata`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.GooglePay),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            WalletType.GooglePay
        )
    }

    @Test
    fun `create filters out wallets disabled by configuration`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .linkVisibility(ExpressCheckoutElement.Configuration.LinkVisibility.Never),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            WalletType.GooglePay
        )
    }

    @Test
    fun `create returns all available wallets`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            WalletType.Link,
            WalletType.GooglePay,
        )
    }

    private fun create(
        availableWallets: List<WalletType>,
        configuration: ExpressCheckoutElement.Configuration = ExpressCheckoutElement.Configuration(),
    ): List<WalletType> {
        return DefaultAvailableExpressButtonTypesFactory().create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                availableWallets = availableWallets,
            ),
            expressCheckoutElementConfiguration = configuration.build(),
        )
    }
}
