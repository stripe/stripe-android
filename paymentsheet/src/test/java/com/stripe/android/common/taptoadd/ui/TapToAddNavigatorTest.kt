package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class TapToAddNavigatorTest {
    @Test
    fun `screen emits initial screen when constructed with initial screen`() = runTest {
        val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor)
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            initialScreen = initialScreen,
            paymentMethodHolder = FakePaymentMethodHolder(paymentMethod = null),
        )

        navigator.screen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
        }
    }

    @Test
    fun `performAction with Close event emits Canceled with null payment selection when no payment method collected`() =
        runTest {
            val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor)
            val navigator = TapToAddNavigator(
                coroutineScope = this,
                initialScreen = initialScreen,
                paymentMethodHolder = FakePaymentMethodHolder(paymentMethod = null),
            )

            navigator.result.test {
                navigator.performAction(TapToAddNavigator.Action.Close)
                assertThat(awaitItem()).isEqualTo(TapToAddResult.Canceled(paymentSelection = null))
            }
        }

    @Test
    fun `performAction with Close event emits Canceled with payment selection when payment method collected`() =
        runTest {
            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor)
            val navigator = TapToAddNavigator(
                coroutineScope = this,
                initialScreen = initialScreen,
                paymentMethodHolder = FakePaymentMethodHolder(paymentMethod = paymentMethod),
            )

            navigator.result.test {
                navigator.performAction(TapToAddNavigator.Action.Close)
                val result = awaitItem() as TapToAddResult.Canceled
                assertThat(result.paymentSelection).isEqualTo(PaymentSelection.Saved(paymentMethod))
            }
        }

    private object FakeTapToAddCollectingInteractor : TapToAddCollectingInteractor
}
