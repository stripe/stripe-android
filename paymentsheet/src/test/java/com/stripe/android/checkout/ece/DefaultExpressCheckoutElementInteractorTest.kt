package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class DefaultExpressCheckoutElementInteractorTest {
    @Test
    fun `state contains provided express buttons`() {
        val interactor = DefaultExpressCheckoutElementInteractor(
            availableExpressButtonTypes = listOf(
                ExpressButton.Link,
                ExpressButton.GooglePay,
            )
        )

        assertThat(interactor.state.value.expressButtons).containsExactly(
            ExpressButton.Link,
            ExpressButton.GooglePay,
        )
    }

    @Test
    fun `create keeps only wallets returned by metadata`() {
        val interactor = createInteractor(
            availableWallets = listOf(WalletType.GooglePay),
        )

        assertThat(interactor.state.value.expressButtons).containsExactly(
            ExpressButton.GooglePay
        )
    }

    @Test
    fun `create filters out wallets disabled by configuration`() {
        val interactor = createInteractor(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
        ) {
            linkVisibility(ExpressCheckoutElement.Configuration.LinkVisibility.Never)
        }

        assertThat(interactor.state.value.expressButtons).containsExactly(
            ExpressButton.GooglePay
        )
    }

    private fun createInteractor(
        availableWallets: List<WalletType>,
        configure: ExpressCheckoutElement.Configuration.() -> Unit = {},
    ): DefaultExpressCheckoutElementInteractor {
        return DefaultExpressCheckoutElementInteractor(emptyList()).create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                availableWallets = availableWallets,
            ),
            expressCheckoutElementConfig = ExpressCheckoutElement.Configuration()
                .apply(configure)
                .build(),
        )
    }
}
