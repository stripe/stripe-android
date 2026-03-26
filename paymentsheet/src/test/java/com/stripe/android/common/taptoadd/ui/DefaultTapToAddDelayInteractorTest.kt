package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultTapToAddDelayInteractorTest {
    @Test
    fun `card brand and last4 are set from payment method`() = runTest {
        val paymentMethod = PaymentMethodFactory.card()
            .update(
                last4 = "1234",
                brand = CardBrand.MasterCard,
                addCbcNetworks = false,
            )

        val interactor = DefaultTapToAddDelayInteractor(
            coroutineContext = coroutineContext,
            paymentMethod = paymentMethod,
            onShown = {},
        )

        assertThat(interactor.cardBrand).isEqualTo(CardBrand.MasterCard)
        assertThat(interactor.last4).isEqualTo("1234")
    }

    @Test
    fun `onShown invokes callback after delay`() = runTest {
        val onShownCalls = Turbine<Unit>()
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        DefaultTapToAddDelayInteractor(
            coroutineContext = coroutineContext,
            paymentMethod = paymentMethod,
            onShown = { onShownCalls.add(Unit) },
        )

        advanceTimeBy(1000L)
        runCurrent()

        assertThat(onShownCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `close before delay elapses does not invoke onShown`() = runTest {
        val onShownCalls = Turbine<Unit>()
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        val interactor = DefaultTapToAddDelayInteractor(
            coroutineContext = coroutineContext,
            paymentMethod = paymentMethod,
            onShown = { onShownCalls.add(Unit) },
        )
        interactor.close()
        advanceTimeBy(1000L)
        runCurrent()
        onShownCalls.ensureAllEventsConsumed()
    }
}
