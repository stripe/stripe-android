package com.stripe.android.paymentelement.embedded.manage

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class EmbeddedNavigatorTest {

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

            val newScreen = EmbeddedNavigator.Screen.ManageUpdate(FakeUpdatePaymentMethodInteractor())
            navigator.performAction(EmbeddedNavigator.Action.GoToScreen(newScreen))
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
        navigator.performAction(EmbeddedNavigator.Action.Back)
        navigator.result.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `navigating back with two screens navigates back`() = testScenario {
        navigator.screen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)

            val newScreen = EmbeddedNavigator.Screen.ManageUpdate(FakeUpdatePaymentMethodInteractor())
            navigator.performAction(EmbeddedNavigator.Action.GoToScreen(newScreen))
            assertThat(awaitItem()).isEqualTo(newScreen)
            assertThat(eventReporter.showEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)

            navigator.performAction(EmbeddedNavigator.Action.Back)
            assertThat(awaitItem()).isEqualTo(initialScreen)
            assertThat(eventReporter.hideEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)
        }

        navigator.result.test {
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `performing close action emits result`() = testScenario {
        navigator.performAction(EmbeddedNavigator.Action.Close())
        navigator.result.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `performing close action with shouldInvokeRowSelection true emits true`() = testScenario {
        navigator.performAction(
            EmbeddedNavigator.Action.Close(
                shouldInvokeRowSelectionCallback = true
            )
        )
        navigator.result.test {
            val item = awaitItem()
            assertThat(item).isNotNull()
            assertThat(item).isTrue()
        }
    }

    @Test
    fun `performing close action with shouldInvokeRowSelection false emits false`() = testScenario {
        navigator.performAction(
            EmbeddedNavigator.Action.Close(
                shouldInvokeRowSelectionCallback = false
            )
        )
        navigator.result.test {
            val item = awaitItem()
            assertThat(item).isNotNull()
            assertThat(item).isFalse()
        }
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        lateinit var navigator: EmbeddedNavigator
        val initialScreen = EmbeddedNavigator.Screen.ManageAll(FakeManageScreenInteractor())
        val eventReporter = FakeEventReporter()
        navigator = EmbeddedNavigator(
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
        val navigator: EmbeddedNavigator,
        val initialScreen: EmbeddedNavigator.Screen,
        val eventReporter: FakeEventReporter,
    )
}
