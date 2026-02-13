package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultTapToAddCardAddedInteractorTest {
    @Test
    fun `card brand & last 4 are set from payment method`() {
        val paymentMethod = PaymentMethodFactory.card()
            .update(
                last4 = "1234",
                brand = CardBrand.MasterCard,
                addCbcNetworks = false,
            )

        val interactor = DefaultTapToAddCardAddedInteractor(
            paymentMethod = paymentMethod,
            onShown = {},
        )

        assertThat(interactor.cardBrand).isEqualTo(CardBrand.MasterCard)
        assertThat(interactor.last4).isEqualTo("1234")
    }

    @Test
    fun `onShown invokes callback`() = runTest {
        val onShownCalls = Turbine<Unit>()
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        val interactor = DefaultTapToAddCardAddedInteractor(
            paymentMethod = paymentMethod,
            onShown = { onShownCalls.add(Unit) },
        )

        interactor.onShown()

        assertThat(onShownCalls.awaitItem()).isEqualTo(Unit)
    }
}
