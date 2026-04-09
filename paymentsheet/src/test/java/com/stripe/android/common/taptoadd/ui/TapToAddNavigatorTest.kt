package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.isInstanceOf
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class TapToAddNavigatorTest {
    @Test
    fun `screen emits initial screen when constructed with initial screen`() = runTest {
        val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor())
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
            val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor())
            val navigator = TapToAddNavigator(
                coroutineScope = this,
                stateHolder = FakeStateHolder(state = null),
                initialScreen = initialScreen,
            )

            navigator.result.test {
                navigator.performAction(TapToAddNavigator.Action.Close())
                assertThat(awaitItem()).isEqualTo(TapToAddResult.Canceled(paymentSelection = null))
            }
        }

    @Test
    fun `performAction with Close event emits Canceled with payment selection when payment method collected`() =
        runTest {
            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor())
            val navigator = TapToAddNavigator(
                coroutineScope = this,
                stateHolder = FakeStateHolder(
                    state = TapToAddStateHolder.State.CardAdded(paymentMethod),
                ),
                initialScreen = initialScreen,
            )

            navigator.result.test {
                navigator.performAction(TapToAddNavigator.Action.Close())
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
            val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor())
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
                navigator.performAction(TapToAddNavigator.Action.Close())
                val result = awaitItem() as TapToAddResult.Canceled
                assertThat(result.paymentSelection).isEqualTo(
                    PaymentSelection.Saved(
                        paymentMethod = paymentMethod,
                        linkInput = userInput
                    )
                )
            }
        }

    @Test
    fun `performAction with CloseWithUnsupportedDevice emits UnsupportedDevice`() = runTest {
        val initialScreen = TapToAddNavigator.Screen.NotSupportedError
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = initialScreen,
        )

        navigator.result.test {
            navigator.performAction(TapToAddNavigator.Action.CloseWithUnsupportedDevice)
            assertThat(awaitItem()).isEqualTo(TapToAddResult.UnsupportedDevice)
        }
    }

    @Test
    fun `Close action should close collecting interactor`() = runTest {
        val interactor = FakeTapToAddCollectingInteractor()
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = TapToAddNavigator.Screen.Collecting(interactor),
        )

        navigator.performAction(TapToAddNavigator.Action.Close())

        interactor.onClose.awaitItem()
        interactor.validate()
    }

    @Test
    fun `Close action should close card added interactor`() = runTest {
        val interactor = FakeTapToAddCardAddedInteractor()
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = TapToAddNavigator.Screen.CardAdded(interactor),
        )

        navigator.performAction(TapToAddNavigator.Action.Close())

        interactor.onClose.awaitItem()
        interactor.validate()
    }

    @Test
    fun `Close action should close delay interactor`() = runTest {
        val interactor = FakeTapToAddDelayInteractor()
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = TapToAddNavigator.Screen.Delay(interactor),
        )

        navigator.performAction(TapToAddNavigator.Action.Close())

        interactor.onClose.awaitItem()
        interactor.ensureAllEventsConsumed()
    }

    @Test
    fun `Close action should close confirmation interactor`() = runTest {
        val interactor = FakeTapToAddConfirmationInteractor()
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = TapToAddNavigator.Screen.Confirmation(interactor),
        )

        navigator.performAction(TapToAddNavigator.Action.Close())

        interactor.onClose.awaitItem()
        interactor.validate()
    }

    @Test
    fun `Close from CardAdded screen cancel button invokes CancelPressed after close`() = runTest {
        val interactor = FakeTapToAddCardAddedInteractor()
        val screen = TapToAddNavigator.Screen.CardAdded(interactor)

        val currentCloseButton = screen.cancelButton.value

        assertThat(currentCloseButton).isInstanceOf<TapToAddNavigator.CancelButton.Available>()

        val availableButton = currentCloseButton as TapToAddNavigator.CancelButton.Available

        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = screen,
        )

        navigator.result.test {
            navigator.performAction(availableButton.action)
            interactor.onClose.awaitItem()
            assertThat(interactor.performActionCalls.awaitItem())
                .isEqualTo(TapToAddCardAddedInteractor.Action.CancelPressed)
            assertThat(awaitItem()).isEqualTo(TapToAddResult.Canceled(paymentSelection = null))
        }
        interactor.validate()
    }

    @Test
    fun `Close from Confirmation screen cancel button invokes CancelPressed after close`() = runTest {
        val interactor = FakeTapToAddConfirmationInteractor()
        val screen = TapToAddNavigator.Screen.Confirmation(interactor)

        val currentCloseButton = screen.cancelButton.value

        assertThat(currentCloseButton).isInstanceOf<TapToAddNavigator.CancelButton.Available>()

        val availableButton = currentCloseButton as TapToAddNavigator.CancelButton.Available

        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = screen,
        )

        navigator.result.test {
            navigator.performAction(availableButton.action)
            interactor.onClose.awaitItem()
            assertThat(interactor.performActionCalls.awaitItem())
                .isEqualTo(TapToAddConfirmationInteractor.Action.CancelPressed)
            assertThat(awaitItem()).isEqualTo(TapToAddResult.Canceled(paymentSelection = null))
        }
        interactor.validate()
    }

    @Test
    fun `Cancel button for Confirmation screen is invisible when primary button is processing`() = runTest {
        val interactor = FakeTapToAddConfirmationInteractor(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Processing,
        )
        val screen = TapToAddNavigator.Screen.Confirmation(interactor)

        assertThat(screen.cancelButton.value).isEqualTo(TapToAddNavigator.CancelButton.Invisible)
    }

    @Test
    fun `Cancel button for Confirmation screen is invisible when primary button is success`() = runTest {
        val interactor = FakeTapToAddConfirmationInteractor(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Success,
        )
        val screen = TapToAddNavigator.Screen.Confirmation(interactor)

        assertThat(screen.cancelButton.value).isEqualTo(TapToAddNavigator.CancelButton.Invisible)
    }

    @Test
    fun `NavigateTo action closes current screen interactor only`() = runTest {
        val collectingInteractor = FakeTapToAddCollectingInteractor()
        val cardAddedInteractor = FakeTapToAddCardAddedInteractor()
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = TapToAddNavigator.Screen.Collecting(collectingInteractor),
        )

        navigator.performAction(
            TapToAddNavigator.Action.NavigateTo(
                TapToAddNavigator.Screen.CardAdded(cardAddedInteractor),
            ),
        )

        collectingInteractor.onClose.awaitItem()
        collectingInteractor.validate()
        cardAddedInteractor.validate()
    }

    @Test
    fun `Complete action closes current interactor`() = runTest {
        val interactor = FakeTapToAddCollectingInteractor()
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = TapToAddNavigator.Screen.Collecting(interactor),
        )

        navigator.performAction(TapToAddNavigator.Action.Complete)

        interactor.onClose.awaitItem()
        interactor.validate()
    }

    @Test
    fun `Continue closes current interactor`() = runTest {
        val interactor = FakeTapToAddCollectingInteractor()
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            stateHolder = FakeStateHolder(state = null),
            initialScreen = TapToAddNavigator.Screen.Collecting(interactor),
        )
        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        navigator.performAction(TapToAddNavigator.Action.Continue(paymentSelection))

        interactor.onClose.awaitItem()
        interactor.validate()
    }
}
