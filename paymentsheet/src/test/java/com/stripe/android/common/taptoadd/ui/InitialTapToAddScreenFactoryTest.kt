package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class InitialTapToAddScreenFactoryTest {
    @Test
    fun `createInitialScreen returns Collecting screen by default`() = scenarioTest(
        paymentMethod = null,
    ) {
        val screen = screenFactory.createInitialScreen()

        assertThat(collectingInteractorFactory.createCalls.awaitItem()).isNotNull()
        assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.Collecting>()

        val collectingScreen = screen as TapToAddNavigator.Screen.Collecting

        assertThat(collectingScreen.interactor).isEqualTo(FakeTapToAddCollectingInteractor)
    }

    @Test
    fun `createInitialScreen returns Confirmation screen when holder contains a payment method`() {
        val paymentMethod = PaymentMethodFactory.card(random = true)

        scenarioTest(
            paymentMethod = paymentMethod,
        ) {
            val screen = screenFactory.createInitialScreen()

            val paymentMethodPassedToFactory = confirmationInteractorFactory.createCalls.awaitItem()

            assertThat(paymentMethodPassedToFactory).isEqualTo(paymentMethod)
            assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.Confirmation>()

            val confirmationScreen = screen as TapToAddNavigator.Screen.Confirmation

            assertThat(confirmationScreen.interactor).isEqualTo(FakeTapToAddConfirmationInteractor)
        }
    }

    private fun scenarioTest(
        paymentMethod: PaymentMethod?,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val paymentMethodHolder = FakePaymentMethodHolder(paymentMethod)
        val collectingInteractorFactory = FakeTapToAddCollectingInteractor.Factory()
        val confirmationInteractorFactory = FakeTapToAddConfirmationInteractor.Factory()

        val screenFactory = InitialTapToAddScreenFactory(
            collectingInteractorFactory = collectingInteractorFactory,
            confirmationInteractorFactory = confirmationInteractorFactory,
            paymentMethodHolder = paymentMethodHolder,
        )

        block(
            Scenario(
                collectingInteractorFactory = collectingInteractorFactory,
                confirmationInteractorFactory = confirmationInteractorFactory,
                screenFactory = screenFactory,
            )
        )

        collectingInteractorFactory.validate()
        confirmationInteractorFactory.validate()
        paymentMethodHolder.validate()
    }

    private object FakeTapToAddConfirmationInteractor : TapToAddConfirmationInteractor {
        override val state: StateFlow<TapToAddConfirmationInteractor.State>
            get() = throw IllegalStateException("Should not be fetched!")

        override fun performAction(action: TapToAddConfirmationInteractor.Action) {
            throw IllegalStateException("Should not be called!")
        }

        class Factory : TapToAddConfirmationInteractor.Factory {
            private val _createCalls = Turbine<PaymentMethod>()
            val createCalls: ReceiveTurbine<PaymentMethod> = _createCalls

            override fun create(paymentMethod: PaymentMethod): TapToAddConfirmationInteractor {
                _createCalls.add(paymentMethod)

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

    private class Scenario(
        val collectingInteractorFactory: FakeTapToAddCollectingInteractor.Factory,
        val confirmationInteractorFactory: FakeTapToAddConfirmationInteractor.Factory,
        val screenFactory: InitialTapToAddScreenFactory,
    )
}
