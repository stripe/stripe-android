package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
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
            stateHolder = FakeStateHolder(state = null),
            initialScreen = initialScreen,
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
                stateHolder = FakeStateHolder(state = null),
                initialScreen = initialScreen,
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
                stateHolder = FakeStateHolder(
                    state = TapToAddStateHolder.State.CardAdded(paymentMethod),
                ),
                initialScreen = initialScreen,
            )

            navigator.result.test {
                navigator.performAction(TapToAddNavigator.Action.Close)
                val result = awaitItem() as TapToAddResult.Canceled
                assertThat(result.paymentSelection).isEqualTo(PaymentSelection.Saved(paymentMethod))
            }
        }

    @Test
    fun `performAction with Close event emits Canceled with payment selection when PM & Link collected`() =
        runTest {
            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val userInput = UserInput.SignUp(
                email = "email@email",
                phone = "2267007611",
                country = "CA",
                name = "John Doe",
                consentAction = SignUpConsentAction.Checkbox,
            )
            val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor)
            val navigator = TapToAddNavigator(
                coroutineScope = this,
                stateHolder = FakeStateHolder(
                    state = TapToAddStateHolder.State.Confirmation(
                        paymentMethod = paymentMethod,
                        linkInput = userInput,
                    ),
                ),
                initialScreen = initialScreen,
            )

            navigator.result.test {
                navigator.performAction(TapToAddNavigator.Action.Close)
                val result = awaitItem() as TapToAddResult.Canceled
                assertThat(result.paymentSelection).isEqualTo(
                    PaymentSelection.Saved(
                        paymentMethod = paymentMethod,
                        linkInput = userInput
                    )
                )
            }
        }

    private object FakeTapToAddCollectingInteractor : TapToAddCollectingInteractor
}
