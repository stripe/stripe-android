package com.stripe.android.paymentelement.embedded.manage

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class ManageNavigatorTest {

    @Test
    fun `initial state is correct`() = testScenario {
        assertThat(navigator.canGoBack).isFalse()
        assertThat(navigator.screen.value).isEqualTo(initialScreen)
        navigator.result.test {
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `navigating to a new screen state is correct`() = testScenario {
        navigator.screen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
            assertThat(navigator.canGoBack).isFalse()

            val newScreen = ManageNavigator.Screen.Update(FakeUpdatePaymentMethodInteractor())
            navigator.performAction(ManageNavigator.Action.GoToScreen(newScreen))
            assertThat(awaitItem()).isEqualTo(newScreen)
            assertThat(navigator.canGoBack).isTrue()
        }
        navigator.result.test {
            ensureAllEventsConsumed()
        }
        assertThat(eventReporter.showEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `navigating back with one screen emits result`() = testScenario {
        navigator.performAction(ManageNavigator.Action.Back)
        navigator.result.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `navigating back with two screens navigates back`() = testScenario {
        navigator.screen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)

            val newScreen = ManageNavigator.Screen.Update(FakeUpdatePaymentMethodInteractor())
            navigator.performAction(ManageNavigator.Action.GoToScreen(newScreen))
            assertThat(awaitItem()).isEqualTo(newScreen)
            assertThat(eventReporter.showEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)

            navigator.performAction(ManageNavigator.Action.Back)
            assertThat(awaitItem()).isEqualTo(initialScreen)
            assertThat(eventReporter.hideEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)
        }

        navigator.result.test {
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `performing close action emits result`() = testScenario {
        navigator.performAction(ManageNavigator.Action.Close)
        navigator.result.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        lateinit var navigator: ManageNavigator
        val initialScreen = ManageNavigator.Screen.All(FakeManageScreenInteractor())
        val eventReporter = FakeEventReporter()
        navigator = ManageNavigator(
            coroutineScope = this,
            eventReporter = eventReporter,
            initialScreen = initialScreen,
        )

        // Initial screen is emitted to event reporter.
        assertThat(eventReporter.showManageSavedPaymentMethods.awaitItem()).isEqualTo(Unit)

        Scenario(
            navigator = navigator,
            initialScreen = initialScreen,
            eventReporter = eventReporter,
        ).block()
        eventReporter.validate()
    }

    private class Scenario(
        val navigator: ManageNavigator,
        val initialScreen: ManageNavigator.Screen,
        val eventReporter: FakeEventReporter,
    )
}
