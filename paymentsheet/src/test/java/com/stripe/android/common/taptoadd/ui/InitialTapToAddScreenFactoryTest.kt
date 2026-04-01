package com.stripe.android.common.taptoadd.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class InitialTapToAddScreenFactoryTest {
    @Test
    fun `createInitialScreen returns Collecting screen by default`() {
        val interactor = FakeTapToAddCollectingInteractor()

        scenarioTest(
            collectingInteractor = interactor,
            initialState = null,
        ) {
            val screen = screenFactory.createInitialScreen()

            assertThat(collectingInteractorFactory.createCalls.awaitItem()).isNotNull()
            assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.Collecting>()

            val collectingScreen = screen as TapToAddNavigator.Screen.Collecting

            assertThat(collectingScreen.interactor).isEqualTo(interactor)
        }
    }

    @Test
    fun `createInitialScreen returns Confirmation screen when holder state is Confirmation`() {
        val interactor = FakeTapToAddConfirmationInteractor()
        val paymentMethod = PaymentMethodFactory.card(random = true)

        scenarioTest(
            confirmationInteractor = interactor,
            initialState = TapToAddStateHolder.State.Confirmation(paymentMethod, linkInput = null),
        ) {
            val screen = screenFactory.createInitialScreen()

            val (paymentMethodPassedToFactory, linkInputPassedToFactory) =
                confirmationInteractorFactory.createCalls.awaitItem()

            assertThat(paymentMethodPassedToFactory).isEqualTo(paymentMethod)
            assertThat(linkInputPassedToFactory).isNull()
            assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.Confirmation>()

            val confirmationScreen = screen as TapToAddNavigator.Screen.Confirmation

            assertThat(confirmationScreen.interactor).isEqualTo(interactor)
        }
    }

    @Test
    fun `createInitialScreen returns CardAdded screen when holder state is CardAdded`() {
        val interactor = FakeTapToAddCardAddedInteractor()
        val paymentMethod = PaymentMethodFactory.card(random = true)

        scenarioTest(
            cardAddedInteractor = interactor,
            initialState = TapToAddStateHolder.State.CardAdded(paymentMethod),
        ) {
            val screen = screenFactory.createInitialScreen()

            val paymentMethodPassedToFactory = cardAddedInteractorFactory.createCalls.awaitItem()

            assertThat(paymentMethodPassedToFactory).isEqualTo(paymentMethod)
            assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.CardAdded>()

            val cardAddedScreen = screen as TapToAddNavigator.Screen.CardAdded

            assertThat(cardAddedScreen.interactor).isEqualTo(interactor)
        }
    }

    private fun scenarioTest(
        collectingInteractor: FakeTapToAddCollectingInteractor = FakeTapToAddCollectingInteractor(),
        cardAddedInteractor: FakeTapToAddCardAddedInteractor = FakeTapToAddCardAddedInteractor(),
        confirmationInteractor: FakeTapToAddConfirmationInteractor = FakeTapToAddConfirmationInteractor(),
        initialState: TapToAddStateHolder.State?,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val stateHolder = FakeStateHolder(state = initialState)
        val collectingInteractorFactory = FakeTapToAddCollectingInteractor.Factory(collectingInteractor)
        val cardAddedInteractorFactory = FakeTapToAddCardAddedInteractor.Factory(cardAddedInteractor)
        val confirmationInteractorFactory = FakeTapToAddConfirmationInteractor.Factory(confirmationInteractor)

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

    private class Scenario(
        val collectingInteractorFactory: FakeTapToAddCollectingInteractor.Factory,
        val confirmationInteractorFactory: FakeTapToAddConfirmationInteractor.Factory,
        val cardAddedInteractorFactory: FakeTapToAddCardAddedInteractor.Factory,
        val screenFactory: InitialTapToAddScreenFactory,
    )
}
