package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Provider

internal class FlowControllerSheetActivityContinueCoordinatorTest {

    @Test
    fun `continue with saved payment method reports the selection and continues`() =
        assertReportsSelectionAndContinues(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

    @Test
    fun `continue with new card without save reports the selection and continues`() =
        assertReportsSelectionAndContinues(
            PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            )
        )

    @Test
    fun `continue with new card with save reports the selection and continues`() =
        assertReportsSelectionAndContinues(
            PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
            )
        )

    @Test
    fun `continue with external payment method reports the selection and continues`() =
        assertReportsSelectionAndContinues(
            PaymentMethodFixtures.createExternalPaymentMethod(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )

    @Test
    fun `continue with custom payment method reports the selection and continues`() =
        assertReportsSelectionAndContinues(
            PaymentMethodFixtures.createCustomPaymentMethod(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD)
        )

    @Test
    fun `continue without a selection clears the error and does nothing else`() = runScenario(selection = null) {
        continueCoordinator.onContinue()

        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isNull()
    }

    @Test
    fun `continue while processing does nothing`() = runScenario {
        stateHolder.updateState { it.copy(isProcessing = true) }

        continueCoordinator.onContinue()
    }

    @Test
    fun `Link selection with RUX enabled presents the Link payment method picker`() = runScenario(
        selection = PaymentSelection.Link(brand = LinkBrand.Link),
        showRuxInFlowController = true,
    ) {
        continueCoordinator.onContinue()

        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isNull()
        assertThat(eventReporter.selectPaymentOptionCalls.awaitItem())
            .isEqualTo(PaymentSelection.Link(brand = LinkBrand.Link))
        assertThat(linkLauncher.presentCalls.awaitItem()).isEqualTo(TestFactory.LINK_CONFIGURATION)
    }

    @Test
    fun `Link selection with RUX disabled continues`() = runScenario(
        selection = PaymentSelection.Link(brand = LinkBrand.Link),
        showRuxInFlowController = false,
    ) {
        continueCoordinator.onContinue()

        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isNull()
        assertThat(eventReporter.selectPaymentOptionCalls.awaitItem())
            .isEqualTo(PaymentSelection.Link(brand = LinkBrand.Link))
        assertThat(defaultContinueCoordinator.onContinueCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `non-Link selection with RUX enabled does not present the Link payment method picker`() = runScenario(
        selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        showRuxInFlowController = true,
    ) {
        continueCoordinator.onContinue()

        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isNull()
        assertThat(eventReporter.selectPaymentOptionCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(defaultContinueCoordinator.onContinueCalls.awaitItem()).isEqualTo(Unit)
    }

    private fun assertReportsSelectionAndContinues(selection: PaymentSelection) = runScenario(
        selection = selection,
    ) {
        continueCoordinator.onContinue()

        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isNull()
        assertThat(eventReporter.selectPaymentOptionCalls.awaitItem()).isEqualTo(selection)
        assertThat(defaultContinueCoordinator.onContinueCalls.awaitItem()).isEqualTo(Unit)
    }

    private fun runScenario(
        selection: PaymentSelection? = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        showRuxInFlowController: Boolean = false,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.NeedsVerification,
                signupMode = null,
            ),
        ),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(selection)
        }
        val stateHolder = FakeSheetActivityStateHolder()
        val eventReporter = FakeEventReporter()
        val defaultContinueCoordinator = FakeSheetActivityContinueCoordinator()
        val linkLauncher = FakeSheetLinkPaymentMethodLauncher()
        val linkGate = FakeLinkGate().apply {
            setShowRuxInFlowController(showRuxInFlowController)
        }
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create()
        val navigator = EmbeddedNavigator(
            coroutineScope = backgroundScope,
            initialScreen = EmbeddedNavigator.Screen.VerticalPaymentOptions(
                interactor = interactor,
                sheetActivityState = stateHolder.state,
                onContinueClick = {},
                onPrimaryButtonDisabledClick = {},
            ),
            analytics = DefaultEmbeddedSheetAnalytics(FakeEventReporter()),
        )
        val continueCoordinator = FlowControllerSheetActivityContinueCoordinator(
            defaultContinueCoordinator = defaultContinueCoordinator,
            paymentMethodMetadata = paymentMethodMetadata,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            embeddedNavigatorProvider = Provider { navigator },
            sheetAnalytics = FlowControllerEmbeddedSheetAnalytics(
                savedStateHandle = SavedStateHandle(),
                eventReporter = eventReporter,
                coroutineScope = backgroundScope,
            ),
            linkLauncher = linkLauncher,
            linkGateFactory = FakeLinkGate.Factory(linkGate),
        )

        Scenario(
            continueCoordinator = continueCoordinator,
            stateHolder = stateHolder,
            eventReporter = eventReporter,
            defaultContinueCoordinator = defaultContinueCoordinator,
            linkLauncher = linkLauncher,
        ).block()

        backgroundScope.cancel()
        assertThat(interactor.closeCalls.awaitItem()).isEqualTo(Unit)
        interactor.validate()
        stateHolder.validate()
        eventReporter.validate()
        defaultContinueCoordinator.validate()
        linkLauncher.validate()
    }

    private class Scenario(
        val continueCoordinator: FlowControllerSheetActivityContinueCoordinator,
        val stateHolder: FakeSheetActivityStateHolder,
        val eventReporter: FakeEventReporter,
        val defaultContinueCoordinator: FakeSheetActivityContinueCoordinator,
        val linkLauncher: FakeSheetLinkPaymentMethodLauncher,
    )
}
