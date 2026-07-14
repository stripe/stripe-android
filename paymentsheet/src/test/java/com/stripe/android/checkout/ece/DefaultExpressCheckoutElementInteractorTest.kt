package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class DefaultExpressCheckoutElementInteractorTest {
    @Test
    fun `default state contains link and google pay buttons`() {
        val interactor = DefaultExpressCheckoutElementInteractor()

        assertThat(interactor.state.value).isEqualTo(
            ExpressCheckoutElementInteractor.State(
                expressButtons = listOf(
                    ExpressButton.Link,
                    ExpressButton.GooglePay,
                )
            )
        )
    }
}
