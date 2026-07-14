package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class DefaultExpressCheckoutElementInteractorTest {
    @Test
    fun `default state contains no wallet buttons`() {
        val interactor = DefaultExpressCheckoutElementInteractor()

        assertThat(interactor.state.value).isEqualTo(
            ExpressCheckoutElementInteractor.State(walletButtons = emptyList())
        )
    }

    @Test
    fun `factory creates default interactor`() {
        val interactor = DefaultExpressCheckoutElementInteractor.Factory.create()

        assertThat(interactor).isInstanceOf(DefaultExpressCheckoutElementInteractor::class.java)
        assertThat(interactor.state.value.walletButtons).isEmpty()
    }
}
