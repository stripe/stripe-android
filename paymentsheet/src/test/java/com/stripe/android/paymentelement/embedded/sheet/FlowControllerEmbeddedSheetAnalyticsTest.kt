package com.stripe.android.paymentelement.embedded.sheet

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.FakeSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeVerticalModeFormInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FlowControllerEmbeddedSheetAnalyticsTest {

    @Test
    fun `saved screen reports existing payment options once`() = runScenario(
        initialScreen = savedScreen(),
    ) {
        assertThat(eventReporter.showExistingPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)

        screen.value = horizontalScreen()
        assertThat(eventReporter.paymentMethodFormShownCalls.awaitItem()).isEqualTo("card")
        screen.value = savedScreen()
    }

    @Test
    fun `vertical payment options reports new payment options once`() = runScenario(
        initialScreen = verticalScreen(),
    ) {
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)

        screen.value = formScreen("card")
        screen.value = verticalScreen()
    }

    @Test
    fun `root form reports form shown and new payment options`() = runScenario(
        initialScreen = formScreen("cashapp"),
    ) {
        assertThat(eventReporter.paymentMethodFormShownCalls.awaitItem()).isEqualTo("cashapp")
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `horizontal payment options reports form shown and new payment options`() = runScenario(
        initialScreen = horizontalScreen(),
    ) {
        assertThat(eventReporter.paymentMethodFormShownCalls.awaitItem()).isEqualTo("card")
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `restored add screen reports form shown again after returning to saved screen`() = runScenario(
        initialScreen = horizontalScreen(),
    ) {
        assertThat(eventReporter.paymentMethodFormShownCalls.awaitItem()).isEqualTo("card")
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)

        screen.value = savedScreen()
        screen.value = horizontalScreen()

        assertThat(eventReporter.paymentMethodFormShownCalls.awaitItem()).isEqualTo("card")
    }

    @Test
    fun `manage screens report every time they are shown and update screen reports hidden`() = runScenario(
        initialScreen = savedScreen(),
    ) {
        assertThat(eventReporter.showExistingPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)

        screen.value = EmbeddedNavigator.Screen.ManageAll(FakeManageScreenInteractor())
        assertThat(eventReporter.showManageSavedPaymentMethods.awaitItem()).isEqualTo(Unit)

        val updateScreen = EmbeddedNavigator.Screen.ManageUpdate(FakeUpdatePaymentMethodInteractor())
        screen.value = updateScreen
        assertThat(eventReporter.showEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)

        analytics.onScreenHidden(updateScreen)
        assertThat(eventReporter.hideEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)

        screen.value = EmbeddedNavigator.Screen.ManageAll(FakeManageScreenInteractor())
        assertThat(eventReporter.showManageSavedPaymentMethods.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `form interaction is reported once per form`() = runScenario {
        analytics.onFormInteraction("card")
        analytics.onFormInteraction("card")
        analytics.onFormInteraction("cashapp")

        assertThat(eventReporter.paymentMethodFormInteractionCalls.awaitItem()).isEqualTo("card")
        assertThat(eventReporter.paymentMethodFormInteractionCalls.awaitItem()).isEqualTo("cashapp")
    }

    @Test
    fun `list primary button press reports confirm button press`() = runScenario {
        analytics.onListPrimaryButtonPressed(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        analytics.onListPrimaryButtonPressed(null)

        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `returned selection reports selected payment option`() = runScenario {
        analytics.onSelectionReturned(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        assertThat(eventReporter.selectPaymentOptionCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `cancelled close reports dismiss`() = runScenario {
        analytics.onClosed(
            EmbeddedActivityResult.Cancelled(
                customerState = null,
                linkAccountInfo = LinkAccountUpdate.Value(null),
                launchMode = LAUNCH_MODE,
            )
        )

        assertThat(eventReporter.dismissCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `completed close does not report dismiss`() = runScenario {
        analytics.onClosed(
            EmbeddedActivityResult.Complete(
                selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
                previousNewSelections = Bundle(),
                hasBeenConfirmed = false,
                customerState = null,
                linkAccountInfo = LinkAccountUpdate.Value(null),
                checkoutSessionResponse = null,
                autocompleteFilledAddress = null,
                shouldInvokeSelectionCallback = false,
                launchMode = LAUNCH_MODE,
            )
        )
        analytics.onClosed(null)
    }

    @Test
    fun `presenting outside the application task reports the deep link event once`() = runScenario {
        analytics.onPresented(isApplicationTaskOwner = false)
        analytics.onPresented(isApplicationTaskOwner = false)

        assertThat(eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `presenting from the application task does not report the deep link event`() = runScenario {
        analytics.onPresented(isApplicationTaskOwner = true)
    }

    private fun runScenario(
        initialScreen: EmbeddedNavigator.Screen? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val eventReporter = FakeEventReporter()
        val analytics = FlowControllerEmbeddedSheetAnalytics(
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            coroutineScope = backgroundScope,
        )
        val screen = MutableStateFlow(initialScreen ?: savedScreen())
        if (initialScreen != null) {
            analytics.attach(screen)
        }

        Scenario(
            analytics = analytics,
            eventReporter = eventReporter,
            screen = screen,
        ).block()

        eventReporter.validate()
    }

    private class Scenario(
        val analytics: FlowControllerEmbeddedSheetAnalytics,
        val eventReporter: FakeEventReporter,
        val screen: MutableStateFlow<EmbeddedNavigator.Screen>,
    )

    private fun savedScreen() = EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions(
        interactor = FakeSelectSavedPaymentMethodsInteractor(),
        sheetActivityState = sheetActivityState,
        mandateText = stateFlowOf(null),
        isContinueButtonVisible = stateFlowOf(false),
        onContinueClick = {},
        onPrimaryButtonDisabledClick = {},
    )

    private fun horizontalScreen() = EmbeddedNavigator.Screen.HorizontalPaymentOptions(
        interactor = FakeAddPaymentMethodInteractor(FakeAddPaymentMethodInteractor.createState()),
        sheetActivityState = sheetActivityState,
        onContinueClick = {},
        onPrimaryButtonDisabledClick = {},
    )

    private fun verticalScreen() = EmbeddedNavigator.Screen.VerticalPaymentOptions(
        interactor = FakePaymentMethodVerticalLayoutInteractor.create(),
        sheetActivityState = sheetActivityState,
        onContinueClick = {},
        onPrimaryButtonDisabledClick = {},
    )

    private fun formScreen(code: PaymentMethodCode) = EmbeddedNavigator.Screen.Form(
        formInteractor = FakeVerticalModeFormInteractor.create(
            paymentMethodCode = code,
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
            ),
        ),
        sheetActivityStateHolder = FakeSheetActivityStateHolder(),
        confirmationHelper = FakeSheetActivityConfirmationHelper(),
        embeddedSelectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
        customerStateHolder = FakeCustomerStateHolder(),
        linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
        launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = code),
    )

    private val sheetActivityState = stateFlowOf(
        SheetActivityStateHolder.State(
            primaryButtonLabel = "Continue".resolvableString,
            isEnabled = true,
            processingState = PrimaryButtonProcessingState.Idle(null),
            isProcessing = false,
            primaryButtonIcon = PrimaryButtonIcon.None,
        )
    )

    private companion object {
        val LAUNCH_MODE = EmbeddedLaunchMode.PaymentOptions(
            flowController = EmbeddedLaunchMode.FlowControllerOptions(
                visibleWallets = emptyList(),
                googlePlacesApiKey = null,
            ),
        )
    }
}
