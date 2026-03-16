package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class InitialTapToAddScreenFactoryTest {
    @Test
    fun `createInitialScreen returns Collecting screen by default`() = scenarioTest(
        initialState = null,
    ) {
        val screen = screenFactory.createInitialScreen()

        assertThat(collectingInteractorFactory.createCalls.awaitItem()).isNotNull()
        assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.Collecting>()

        val collectingScreen = screen as TapToAddNavigator.Screen.Collecting

        assertThat(collectingScreen.interactor).isEqualTo(FakeTapToAddCollectingInteractor)
    }

    @Test
    fun `createInitialScreen returns Confirmation screen when holder state is Confirmation`() {
        val paymentMethod = PaymentMethodFactory.card(random = true)

        scenarioTest(
            initialState = TapToAddStateHolder.State.Confirmation(paymentMethod, linkInput = null),
        ) {
            val screen = screenFactory.createInitialScreen()

            val (paymentMethodPassedToFactory, linkInputPassedToFactory) =
                confirmationInteractorFactory.createCalls.awaitItem()

            assertThat(paymentMethodPassedToFactory).isEqualTo(paymentMethod)
            assertThat(linkInputPassedToFactory).isNull()
            assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.Confirmation>()

            val confirmationScreen = screen as TapToAddNavigator.Screen.Confirmation

            assertThat(confirmationScreen.interactor).isEqualTo(FakeTapToAddConfirmationInteractor)
        }
    }

    @Test
    fun `createInitialScreen returns CardAdded screen when holder state is CardAdded`() {
        val paymentMethod = PaymentMethodFactory.card(random = true)

        scenarioTest(
            initialState = TapToAddStateHolder.State.CardAdded(paymentMethod),
        ) {
            val screen = screenFactory.createInitialScreen()

            val paymentMethodPassedToFactory = cardAddedInteractorFactory.createCalls.awaitItem()

            assertThat(paymentMethodPassedToFactory).isEqualTo(paymentMethod)
            assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.CardAdded>()

            val cardAddedScreen = screen as TapToAddNavigator.Screen.CardAdded

            assertThat(cardAddedScreen.interactor).isEqualTo(FakeTapToAddCardAddedInteractor)
        }
    }

    private fun scenarioTest(
        initialState: TapToAddStateHolder.State?,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val stateHolder = FakeStateHolder(state = initialState)
        val collectingInteractorFactory = FakeTapToAddCollectingInteractor.Factory()
        val cardAddedInteractorFactory = FakeTapToAddCardAddedInteractor.Factory()
        val confirmationInteractorFactory = FakeTapToAddConfirmationInteractor.Factory()

        val screenFactory = InitialTapToAddScreenFactory(
            paymentMethodHolder = stateHolder,
            collectingInteractorFactory = collectingInteractorFactory,
            confirmationInteractorFactory = confirmationInteractorFactory,
            cardAddedInteractorFactory = cardAddedInteractorFactory,
        )

        block(
            Scenario(
                collectingInteractorFactory = collectingInteractorFactory,
                confirmationInteractorFactory = confirmationInteractorFactory,
                cardAddedInteractorFactory = cardAddedInteractorFactory,
                screenFactory = screenFactory,
            )
        )

        collectingInteractorFactory.validate()
        confirmationInteractorFactory.validate()
        cardAddedInteractorFactory.validate()
        stateHolder.validate()
    }

    private object FakeTapToAddConfirmationInteractor : TapToAddConfirmationInteractor {
        override val state: StateFlow<TapToAddConfirmationInteractor.State>
            get() = throw IllegalStateException("Should not be fetched!")

        override fun performAction(action: TapToAddConfirmationInteractor.Action) {
            throw IllegalStateException("Should not be called!")
        }

        class Factory : TapToAddConfirmationInteractor.Factory {
            private val _createCalls = Turbine<Pair<PaymentMethod, UserInput?>>()
            val createCalls: ReceiveTurbine<Pair<PaymentMethod, UserInput?>> = _createCalls

            override fun create(
                paymentMethod: PaymentMethod,
                linkInput: UserInput?,
            ): TapToAddConfirmationInteractor {
                _createCalls.add(paymentMethod to linkInput)

                return FakeTapToAddConfirmationInteractor
            }

            fun validate() {
                _createCalls.ensureAllEventsConsumed()
            }
        }
    }

    private object FakeTapToAddCollectingInteractor : TapToAddCollectingInteractor {
        class Factory : TapToAddCollectingInteractor.Factory {
            private val _createCalls = Turbine<Unit>()
            val createCalls: ReceiveTurbine<Unit> = _createCalls

            override fun create(): TapToAddCollectingInteractor {
                _createCalls.add(Unit)

                return FakeTapToAddCollectingInteractor
            }

            fun validate() {
                _createCalls.ensureAllEventsConsumed()
            }
        }
    }

    private object FakeTapToAddCardAddedInteractor : TapToAddCardAddedInteractor {
        override val state: StateFlow<TapToAddCardAddedInteractor.State>
            get() = throw IllegalStateException("Should not be fetched!")

        override fun performAction(action: TapToAddCardAddedInteractor.Action) {
            throw IllegalStateException("Should not be called!")
        }

        class Factory : TapToAddCardAddedInteractor.Factory {
            private val _createCalls = Turbine<PaymentMethod>()
            val createCalls: ReceiveTurbine<PaymentMethod> = _createCalls

            override fun create(
                paymentMethod: PaymentMethod,
            ): TapToAddCardAddedInteractor {
                _createCalls.add(paymentMethod)

                return FakeTapToAddCardAddedInteractor
            }

            fun validate() {
                _createCalls.ensureAllEventsConsumed()
            }
        }
    }

    private class Scenario(
        val collectingInteractorFactory: FakeTapToAddCollectingInteractor.Factory,
        val confirmationInteractorFactory: FakeTapToAddConfirmationInteractor.Factory,
        val cardAddedInteractorFactory: FakeTapToAddCardAddedInteractor.Factory,
        val screenFactory: InitialTapToAddScreenFactory,
    )
}
