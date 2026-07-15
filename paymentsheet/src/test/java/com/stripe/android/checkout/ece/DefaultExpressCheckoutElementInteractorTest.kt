package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class DefaultExpressCheckoutElementInteractorTest {
    @Test
    fun `state contains provided express buttons`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            availableWallets = listOf(
                WalletType.Link,
                WalletType.GooglePay,
            )
        )
        val interactor = createInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
        )

        assertThat(interactor.state.value.expressButtons).containsExactly(
            ExpressButton.Link.create(
                paymentMethodMetadata,
            ),
            ExpressButton.GooglePay.create(
                paymentMethodMetadata,
            ),
        )
    }

    @Test
    fun `create keeps only wallets returned by metadata`() {
        val interactor = createInteractor(
            availableWallets = listOf(WalletType.GooglePay),
        )

        assertThat(interactor.availableExpressButtonTypes).containsExactly(
            WalletType.GooglePay
        )
    }

    @Test
    fun `create filters out wallets disabled by configuration`() {
        val interactor = createInteractor(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .linkVisibility(ExpressCheckoutElement.Configuration.LinkVisibility.Never),
        )

        assertThat(interactor.availableExpressButtonTypes).containsExactly(
            WalletType.GooglePay
        )
    }

    @Test
    fun `create returns all available wallets()`() {
        val interactor = createInteractor(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
        )

        assertThat(interactor.availableExpressButtonTypes).containsExactly(
            WalletType.Link,
            WalletType.GooglePay,
        )
    }

    private fun createInteractor(
        availableWallets: List<WalletType> = emptyList(),
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            availableWallets = availableWallets,
        ),
        configuration: ExpressCheckoutElement.Configuration = ExpressCheckoutElement.Configuration(),
    ): DefaultExpressCheckoutElementInteractor {
        return DefaultExpressCheckoutElementInteractor.create(
            paymentMethodMetadata,
            expressCheckoutElementConfig = configuration.build(),
        )
    }
}
