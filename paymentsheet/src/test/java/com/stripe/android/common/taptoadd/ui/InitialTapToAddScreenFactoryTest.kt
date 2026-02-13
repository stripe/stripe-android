package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class InitialTapToAddScreenFactoryTest {
    @Test
    fun `createInitialScreen returns Collecting screen by default`() = runTest {
        val collectingInteractorFactory = FakeTapToAddCollectingInteractor.Factory()

        val screenFactory = InitialTapToAddScreenFactory(
            collectingInteractorFactory = collectingInteractorFactory,
        )

        val screen = screenFactory.createInitialScreen()

        assertThat(collectingInteractorFactory.createCalls.awaitItem()).isNotNull()
        assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.Collecting>()

        val collectingScreen = screen as TapToAddNavigator.Screen.Collecting

        assertThat(collectingScreen.interactor).isEqualTo(FakeTapToAddCollectingInteractor)

        collectingInteractorFactory.validate()
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
}
