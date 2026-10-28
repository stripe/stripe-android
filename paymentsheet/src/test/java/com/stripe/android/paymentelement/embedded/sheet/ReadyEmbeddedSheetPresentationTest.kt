package com.stripe.android.paymentelement.embedded.sheet

import android.os.Bundle
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.manage.ManageScreenSavedPaymentMethodSelector
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ReadyEmbeddedSheetPresentationTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `dismissal and back are blocked while processing and resume afterwards`() = runScenario {
        presentation.register()
        registrar.registerCalls.awaitItem()
        navigator.result.test {
            assertThat(presentation.canDismiss()).isTrue()
            stateHolder.updateState { it.copy(isProcessing = true) }

            assertThat(presentation.canDismiss()).isFalse()
            activity.onBackPressedDispatcher.onBackPressed()
            expectNoEvents()

            stateHolder.updateState { it.copy(isProcessing = false) }
            assertThat(presentation.canDismiss()).isTrue()
            activity.onBackPressedDispatcher.onBackPressed()
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `dismissal and back are blocked by a screen network operation`() = runScenario(
        selectionState = SavedPaymentMethodSelectionState.Pending("pm_123"),
    ) {
        presentation.register()
        registrar.registerCalls.awaitItem()
        assertThat(stateHolder.state.value.isProcessing).isFalse()
        assertThat(presentation.canDismiss()).isFalse()
        navigator.result.test {
            activity.onBackPressedDispatcher.onBackPressed()
            expectNoEvents()
        }
    }

    @Suppress("LongMethod")
    private fun runScenario(
        selectionState: SavedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val activity = Robolectric.buildActivity(EmbeddedSheetActivity::class.java).get()
        val metadata = PaymentMethodMetadataFactory.create()
        val stateHolder = FakeSheetActivityStateHolder()
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val customerStateHolder = FakeCustomerStateHolder()
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val eventReporter = FakeEventReporter()
        val manageInteractor = FakeManageScreenInteractor()
        val screen = EmbeddedNavigator.Screen.ManageAll(
            FakeManageScreenInteractor(
                initialState = manageInteractor.state.value.copy(selectionState = selectionState),
            ),
        )
        val navigator = EmbeddedNavigator(
            coroutineScope = backgroundScope,
            initialScreen = screen,
            eventReporter = eventReporter,
        )
        assertThat(eventReporter.showManageSavedPaymentMethods.awaitItem()).isEqualTo(Unit)
        val registrar = FakeSheetActivityRegistrar()
        val continueCoordinator = FakeSheetActivityContinueCoordinator()
        val presentation = ReadyEmbeddedSheetPresentation(
            activity = activity,
            args = EmbeddedActivityArgs(
                paymentMethodMetadata = metadata,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example").build(),
                productUsage = emptySet(),
                paymentElementCallbackIdentifier = "test",
                statusBarColor = null,
                selection = null,
                previousNewSelections = Bundle(),
                customerState = null,
                linkAccountInfo = LinkAccountUpdate.Value(null),
                promotions = emptyList(),
                launchMode = EmbeddedLaunchMode.Manage,
                presentationState = EmbeddedActivityArgs.PresentationState.Ready,
            ),
            activityResultCaller = activity,
            eventReporter = eventReporter,
            customerStateHolder = customerStateHolder,
            linkAccountHolder = linkAccountHolder,
            embeddedNavigator = navigator,
            selectionHolder = selectionHolder,
            sheetActivityRegistrar = registrar,
            sheetActivityStateHolder = stateHolder,
            savedPaymentMethodSelector = FakeManageScreenSavedPaymentMethodSelector(),
            walletsInteractor = PaymentOptionsWalletsInteractor(
                paymentMethodMetadata = metadata,
                customerStateHolder = customerStateHolder,
                selectionHolder = selectionHolder,
                linkAccountHolder = linkAccountHolder,
                continueCoordinator = continueCoordinator,
                sheetActivityStateHolder = stateHolder,
            ),
        )

        Scenario(presentation, activity, stateHolder, navigator, registrar).block()

        registrar.registerCalls.ensureAllEventsConsumed()
        stateHolder.validate()
        customerStateHolder.validate()
        eventReporter.validate()
        continueCoordinator.validate()
    }

    private data class Scenario(
        val presentation: ReadyEmbeddedSheetPresentation,
        val activity: EmbeddedSheetActivity,
        val stateHolder: FakeSheetActivityStateHolder,
        val navigator: EmbeddedNavigator,
        val registrar: FakeSheetActivityRegistrar,
    )

    private class FakeSheetActivityRegistrar : SheetActivityRegistrar {
        val registerCalls = Turbine<Registration>()

        override fun registerAndBootstrap(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
            registerCalls.add(Registration(activityResultCaller, lifecycleOwner))
        }

        data class Registration(val activityResultCaller: ActivityResultCaller, val lifecycleOwner: LifecycleOwner)
    }

    private class FakeManageScreenSavedPaymentMethodSelector : ManageScreenSavedPaymentMethodSelector {
        override val selectionState: StateFlow<SavedPaymentMethodSelectionState> =
            stateFlowOf(SavedPaymentMethodSelectionState.Idle)
        override val checkoutSessionResponse: CheckoutSessionResponse? = null

        override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> = error("Unexpected selection")

        override suspend fun syncBillingAfterEdit(original: PaymentMethod, updated: PaymentMethod): Result<Unit> =
            error("Unexpected billing update")

        override fun clearError() = error("Unexpected clear error")
    }
}
