@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

class DefaultAvailableExpressButtonTypesFactoryTest {
    @Test
    fun `create keeps only express button types returned by metadata`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.GooglePay),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        )
    }

    @Test
    fun `create filters out google pay when configuration is missing`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.GooglePay),
            includeGooglePayConfiguration = false,
        )

        assertThat(availableExpressButtonTypes).isEmpty()
    }

    @Test
    fun `create filters out buttons disabled by configuration`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .linkVisibility(ExpressCheckoutElement.Configuration.LinkVisibility.Never),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        )
    }

    @Test
    fun `create returns all available express button types`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.Link,
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        )
    }

    @Test
    fun `create keeps google pay when shipping address is required`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .shippingAddressRequired(true),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        )
    }

    @Test
    fun `create filters out link when shipping address is required`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link),
            configuration = ExpressCheckoutElement.Configuration()
                .shippingAddressRequired(true),
        )

        assertThat(availableExpressButtonTypes).isEmpty()
    }

    private fun create(
        availableWallets: List<WalletType>,
        configuration: ExpressCheckoutElement.Configuration = ExpressCheckoutElement.Configuration(),
        includeGooglePayConfiguration: Boolean = true,
    ): List<ExpressButtonType> {
        if (includeGooglePayConfiguration) {
            configuration.googlePayConfiguration(
                GooglePayConfiguration(GooglePayConfiguration.Environment.Test)
            )
        }
        return DefaultAvailableExpressButtonTypesFactory().create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                availableWallets = availableWallets,
            ),
            expressCheckoutElementConfiguration = configuration.build(),
        )
    }

    private companion object {
        val TEST_GOOGLE_PAY_CONFIGURATION = GooglePayConfiguration(
            GooglePayConfiguration.Environment.Test,
        ).build()
    }
}
