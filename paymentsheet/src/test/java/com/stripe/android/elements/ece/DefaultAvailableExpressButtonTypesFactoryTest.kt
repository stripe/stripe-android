@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

class DefaultAvailableExpressButtonTypesFactoryTest {
    @Test
    fun `create returns no express button types when ECE configuration is absent`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = null,
        )

        assertThat(availableExpressButtonTypes).isEmpty()
    }

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
    fun `create filters out Google Pay when ECE disables it`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .googlePayConfiguration(
                    GooglePayConfiguration().display(GooglePayConfiguration.Display.Never)
                ),
        )

        assertThat(availableExpressButtonTypes).isEmpty()
    }

    @Test
    fun `create filters out Link when disabled by configuration`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .linkConfiguration(
                    ExpressCheckoutElement.Configuration.LinkConfiguration()
                        .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never)
                ),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        )
    }

    @Test
    fun `create filters out Link when its wallet button is hidden`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link),
            configuration = ExpressCheckoutElement.Configuration()
                .linkConfiguration(
                    ExpressCheckoutElement.Configuration.LinkConfiguration()
                        .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.WalletButtonHidden)
                ),
        )

        assertThat(availableExpressButtonTypes).isEmpty()
    }

    @Test
    fun `create returns all available express button types`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.Link,
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        ).inOrder()
    }

    @Test
    fun `create applies configured payment method order`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .paymentMethodOrder(
                    listOf(
                        ExpressCheckoutElement.PaymentMethod.GooglePay(),
                        ExpressCheckoutElement.PaymentMethod.Link(),
                    )
                ),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
            ExpressButtonType.Link,
        ).inOrder()
    }

    @Test
    fun `create appends payment methods omitted from configured order`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.GooglePay, WalletType.Link),
            configuration = ExpressCheckoutElement.Configuration()
                .paymentMethodOrder(listOf(ExpressCheckoutElement.PaymentMethod.Link())),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.Link,
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        ).inOrder()
    }

    @Test
    fun `create ignores unavailable payment methods in configured order`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link),
            configuration = ExpressCheckoutElement.Configuration()
                .paymentMethodOrder(
                    listOf(
                        ExpressCheckoutElement.PaymentMethod.GooglePay(),
                        ExpressCheckoutElement.PaymentMethod.Link(),
                    )
                ),
        )

        assertThat(availableExpressButtonTypes).containsExactly(ExpressButtonType.Link)
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
        configuration: ExpressCheckoutElement.Configuration? = ExpressCheckoutElement.Configuration(),
    ): List<ExpressButtonType> {
        return DefaultAvailableExpressButtonTypesFactory().create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                availableWallets = availableWallets,
            ),
            expressCheckoutElementConfiguration = configuration?.build(),
        )
    }

    private companion object {
        val TEST_GOOGLE_PAY_CONFIGURATION = GooglePayConfiguration().build()
    }
}
