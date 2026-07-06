package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentelement.embedded.sheet.FakeSheetActivityConfirmationHelper
import com.stripe.android.paymentelement.embedded.sheet.FakeSheetActivityStateHolder
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import kotlinx.coroutines.flow.StateFlow
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

    @Test
    fun `GoToScreen with Form does not emit show analytics`() = testScenario {
        val (formScreen, formInteractor) = createFormScreen()
        navigator.performAction(EmbeddedNavigator.Action.GoToScreen(formScreen))
        formInteractor.validate()
    }

    @Test
    fun `Back from Form screen does not emit hide analytics`() = testScenario {
        val (formScreen, formInteractor) = createFormScreen()
        navigator.screen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
            navigator.performAction(EmbeddedNavigator.Action.GoToScreen(formScreen))
            assertThat(awaitItem()).isEqualTo(formScreen)
            navigator.performAction(EmbeddedNavigator.Action.Back)
            assertThat(awaitItem()).isEqualTo(initialScreen)
        }
        formInteractor.validate()
    }

    @Test
    fun `Close while on ManageUpdate calls onHideEditablePaymentOption`() = testScenario {
        val manageUpdateScreen = EmbeddedNavigator.Screen.ManageUpdate(FakeUpdatePaymentMethodInteractor())
        navigator.screen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
            navigator.performAction(EmbeddedNavigator.Action.GoToScreen(manageUpdateScreen))
            assertThat(awaitItem()).isEqualTo(manageUpdateScreen)
        }
        assertThat(eventReporter.showEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)

        navigator.performAction(EmbeddedNavigator.Action.Close())
        assertThat(eventReporter.hideEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `initial screen ManageUpdate calls onShowEditablePaymentOption`() = runTest {
        val eventReporter = FakeEventReporter()
        EmbeddedNavigator(
            coroutineScope = this,
            eventReporter = eventReporter,
            initialScreen = EmbeddedNavigator.Screen.ManageUpdate(FakeUpdatePaymentMethodInteractor()),
        )
        assertThat(eventReporter.showEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)
        eventReporter.validate()
    }

    @Test
    fun `ManageAll topBarState maps from interactor state`() {
        val interactor = FakeManageScreenInteractor(
            initialState = ManageScreenInteractor.State(
                paymentMethods = emptyList(),
                currentSelection = null,
                isEditing = false,
                canEdit = true,
                linkBrand = LinkBrand.Link,
            )
        )
        val screen = EmbeddedNavigator.Screen.ManageAll(interactor)

        val topBarState = screen.topBarState().value!!
        assertThat(topBarState.showTestModeLabel).isFalse()
        assertThat(topBarState.showEditMenu).isTrue()
        assertThat(topBarState.isEditing).isFalse()
    }

    @Test
    fun `ManageAll title maps from interactor state`() {
        val interactor = FakeManageScreenInteractor(
            initialState = ManageScreenInteractor.State(
                paymentMethods = emptyList(),
                currentSelection = null,
                isEditing = false,
                canEdit = true,
                linkBrand = LinkBrand.Link,
            )
        )
        val screen = EmbeddedNavigator.Screen.ManageAll(interactor)

        val title = screen.title().value
        assertThat(title).isEqualTo(interactor.state.value.title)
    }

    @Test
    fun `ManageAll isPerformingNetworkOperation returns false`() {
        val screen = EmbeddedNavigator.Screen.ManageAll(FakeManageScreenInteractor())
        assertThat(screen.isPerformingNetworkOperation()).isFalse()
    }

    @Test
    fun `ManageAll close calls interactor close`() = runTest {
        val interactor = FakeManageScreenInteractor()
        val screen = EmbeddedNavigator.Screen.ManageAll(interactor)

        screen.close()
        assertThat(interactor.closeCalls.awaitItem()).isEqualTo(Unit)
        interactor.validate()
    }

    @Test
    fun `ManageUpdate topBarState returns interactor topBarState`() {
        val interactor = FakeUpdatePaymentMethodInteractor()
        val screen = EmbeddedNavigator.Screen.ManageUpdate(interactor)

        val topBarState = screen.topBarState().value
        assertThat(topBarState).isEqualTo(interactor.topBarState)
    }

    @Test
    fun `ManageUpdate title returns interactor screenTitle`() {
        val interactor = FakeUpdatePaymentMethodInteractor()
        val screen = EmbeddedNavigator.Screen.ManageUpdate(interactor)

        val title = screen.title().value
        assertThat(title).isEqualTo(interactor.screenTitle)
    }

    @Test
    fun `ManageUpdate isPerformingNetworkOperation when idle returns false`() {
        val interactor = FakeUpdatePaymentMethodInteractor(
            initialState = UpdatePaymentMethodInteractor.State(
                error = null,
                status = UpdatePaymentMethodInteractor.Status.Idle,
                setAsDefaultCheckboxChecked = false,
                isSaveButtonEnabled = false,
            )
        )
        val screen = EmbeddedNavigator.Screen.ManageUpdate(interactor)
        assertThat(screen.isPerformingNetworkOperation()).isFalse()
    }

    @Test
    fun `ManageUpdate isPerformingNetworkOperation when updating returns true`() {
        val interactor = FakeUpdatePaymentMethodInteractor(
            initialState = UpdatePaymentMethodInteractor.State(
                error = null,
                status = UpdatePaymentMethodInteractor.Status.Updating,
                setAsDefaultCheckboxChecked = false,
                isSaveButtonEnabled = false,
            )
        )
        val screen = EmbeddedNavigator.Screen.ManageUpdate(interactor)
        assertThat(screen.isPerformingNetworkOperation()).isTrue()
    }

    private fun createFormScreen(): Pair<EmbeddedNavigator.Screen.Form, TestFormInteractor> {
        val formInteractor = TestFormInteractor()
        val screen = EmbeddedNavigator.Screen.Form(
            formInteractor = formInteractor,
            eventReporter = FakeEventReporter(),
            sheetActivityStateHolder = FakeSheetActivityStateHolder(),
            confirmationHelper = FakeSheetActivityConfirmationHelper(),
            embeddedSelectionHolder = EmbeddedSelectionHolder(SavedStateHandle()),
            savedPaymentMethodConfirmInteractorFactory = FakeSavedPaymentMethodConfirmInteractor.Factory(),
            customerStateHolder = FakeCustomerStateHolder(),
            launchMode = EmbeddedLaunchMode.Form,
        )
        return screen to formInteractor
    }

    private class TestFormInteractor : VerticalModeFormInteractor {
        override val isLiveMode: Boolean = true
        override val state: StateFlow<VerticalModeFormInteractor.State>
            get() = throw AssertionError("Not expected")

        val handleViewActionCalls = Turbine<VerticalModeFormInteractor.ViewAction>()
        val closeCalls = Turbine<Unit>()

        override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
            handleViewActionCalls.add(viewAction)
        }

        override fun close() {
            closeCalls.add(Unit)
        }

        fun validate() {
            handleViewActionCalls.ensureAllEventsConsumed()
            closeCalls.ensureAllEventsConsumed()
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
