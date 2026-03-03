package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultTapToAddCompletedInteractorTest {
    @Test
    fun `card brand, last4, and label are set from payment method & amount`() = runTest {
        val paymentMethod = PaymentMethodFactory.card()
            .update(
                last4 = "1234",
                brand = CardBrand.MasterCard,
                addCbcNetworks = false,
            )

        val interactor = DefaultTapToAddCompletedInteractor(
            coroutineScope = this,
            amount = Amount(
                value = 5099,
                currencyCode = "CAD"
            ),
            paymentMethod = paymentMethod,
            onShown = {},
        )

        assertThat(interactor.cardBrand).isEqualTo(CardBrand.MasterCard)
        assertThat(interactor.last4).isEqualTo("1234")
        assertThat(interactor.label).isEqualTo(
            resolvableString(
                R.string.stripe_tap_to_add_paid_label,
                "CA$50.99"
            )
        )
    }

    @Test
    fun `onShown invokes callback after delay`() = runTest {
        val onShownCalls = Turbine<Unit>()
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        DefaultTapToAddCompletedInteractor(
            coroutineScope = this,
            amount = null,
            paymentMethod = paymentMethod,
            onShown = { onShownCalls.add(Unit) },
        )

        advanceTimeBy(2500L)
        runCurrent()

        assertThat(onShownCalls.awaitItem()).isEqualTo(Unit)
    }
}
