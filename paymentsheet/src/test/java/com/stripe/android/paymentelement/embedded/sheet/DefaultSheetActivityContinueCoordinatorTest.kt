package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeFraudDetectionDataRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultSheetActivityContinueCoordinatorTest {

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `without tax region update returns complete synchronously`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    ) {
        continueCoordinator.onContinue()

        assertThat(stateHolder.resultTurbine.awaitItem()).isEqualTo(
            completeResult(checkoutSessionResponse = null)
        )
        stateHolder.updateProcessingTurbine.expectNoEvents()
    }

    @Test
    fun `continue without tax region update is ignored while processing`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    ) {
        stateHolder.updateState { it.copy(isProcessing = true) }

        continueCoordinator.onContinue()

        stateHolder.resultTurbine.expectNoEvents()
        stateHolder.updateProcessingTurbine.expectNoEvents()
    }

    @Test
    fun `repeated continue is ignored before tax region update coroutine starts`() {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        runScenario(paymentMethodMetadata = CHECKOUT_SESSION_METADATA) {
            continueCoordinator.onContinue()

            assertThat(stateHolder.state.value.isProcessing).isTrue()
            assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()

            continueCoordinator.onContinue()

            stateHolder.updateProcessingTurbine.expectNoEvents()
            stateHolder.resultTurbine.expectNoEvents()
            testScope.runCurrent()

            val result = stateHolder.resultTurbine.awaitItem() as EmbeddedActivityResult.Complete
            assertThat(result.checkoutSessionResponse?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
            stateHolder.resultTurbine.expectNoEvents()
            stateHolder.updateProcessingTurbine.expectNoEvents()
        }
    }

    @Test
    fun `successful tax region update returns updated response`() {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        runScenario(paymentMethodMetadata = CHECKOUT_SESSION_METADATA) {
            continueCoordinator.onContinue()
            selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            testScope.runCurrent()

            assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()
            val result = stateHolder.resultTurbine.awaitItem() as EmbeddedActivityResult.Complete
            assertThat(result).isEqualTo(
                completeResult(
                    selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
                    checkoutSessionResponse = result.checkoutSessionResponse,
                )
            )
            assertThat(result.checkoutSessionResponse?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        }
    }

    @Test
    fun `failed tax region update shows error and allows retry`() {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"$ERROR_MESSAGE"}}""")
        }

        runScenario(paymentMethodMetadata = CHECKOUT_SESSION_METADATA) {
            continueCoordinator.onContinue()
            testScope.runCurrent()

            assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()
            assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isFalse()
            assertThat(stateHolder.updateErrorTurbine.awaitItem())
                .isEqualTo(IllegalStateException(ERROR_MESSAGE).stripeErrorMessage())
            stateHolder.resultTurbine.expectNoEvents()
            assertThat(stateHolder.state.value.isProcessing).isFalse()

            networkRule.checkoutUpdate { response ->
                response.testBodyFromFile("checkout-session-init.json")
            }
            continueCoordinator.onContinue()
            testScope.runCurrent()

            assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()
            val result = stateHolder.resultTurbine.awaitItem() as EmbeddedActivityResult.Complete
            assertThat(result.checkoutSessionResponse?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        }
    }

    @Test
    fun `Continue does not start a tax update during queued Manage navigation and works after returning`() {
        val updateCalls = Turbine<Unit>()
        val response = CheckoutSessionResponseFactory.create()
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            taxRegionUpdater = SheetTaxRegionUpdater { _, _, _ ->
                updateCalls.add(Unit)
                Result.success(response)
            },
        ) {
            val manageInteractor = FakeManageScreenInteractor()
            val manageScreen = EmbeddedNavigator.Screen.ManageAll(manageInteractor)
            navigator.performAction(EmbeddedNavigator.Action.GoToScreen(manageScreen))
            assertThat(eventReporter.showManageSavedPaymentMethods.awaitItem()).isEqualTo(Unit)
            assertThat(navigator.screen.value).isInstanceOf(EmbeddedNavigator.Screen.VerticalPaymentOptions::class.java)

            continueCoordinator.onContinue()
            testScope.runCurrent()

            assertThat(stateHolder.state.value.isProcessing).isFalse()
            stateHolder.updateProcessingTurbine.expectNoEvents()
            stateHolder.resultTurbine.expectNoEvents()
            updateCalls.expectNoEvents()

            testScope.advanceTimeBy(250)
            testScope.runCurrent()
            assertThat(navigator.screen.value).isSameInstanceAs(manageScreen)
            navigator.performAction(EmbeddedNavigator.Action.Back)
            assertThat(manageInteractor.closeCalls.awaitItem()).isEqualTo(Unit)
            manageInteractor.validate()

            continueCoordinator.onContinue()
            testScope.runCurrent()

            assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()
            assertThat(updateCalls.awaitItem()).isEqualTo(Unit)
            assertThat(stateHolder.resultTurbine.awaitItem()).isEqualTo(
                completeResult(checkoutSessionResponse = response)
            )
        }
        updateCalls.ensureAllEventsConsumed()
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata,
        selection: PaymentSelection? = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        taxRegionUpdater: SheetTaxRegionUpdater = SheetTaxRegionUpdater(checkoutSessionTaxRegionUpdater()),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(selection)
        }
        val customerStateHolder = FakeCustomerStateHolder()
        val stateHolder = FakeSheetActivityStateHolder()
        val eventReporter = FakeEventReporter()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create()
        val navigator = EmbeddedNavigator(
            coroutineScope = backgroundScope,
            initialScreen = EmbeddedNavigator.Screen.VerticalPaymentOptions(
                interactor = interactor,
                sheetActivityState = stateHolder.state,
                onContinueClick = {},
                onPrimaryButtonDisabledClick = {},
            ),
            eventReporter = eventReporter,
        )
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
        val continueCoordinator = DefaultSheetActivityContinueCoordinator(
            taxRegionUpdater = taxRegionUpdater,
            paymentMethodMetadata = paymentMethodMetadata,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            launchMode = LAUNCH_MODE,
            embeddedNavigatorProvider = Provider { navigator },
            coroutineScope = this,
        )

        Scenario(
            continueCoordinator = continueCoordinator,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            testScope = this,
            navigator = navigator,
            eventReporter = eventReporter,
        ).block()

        backgroundScope.cancel()
        assertThat(interactor.closeCalls.awaitItem()).isEqualTo(Unit)
        interactor.validate()
        eventReporter.validate()
        stateHolder.validate()
        customerStateHolder.validate()
    }

    private fun checkoutSessionTaxRegionUpdater(): CheckoutSessionTaxRegionUpdater {
        val checkoutSessionRepository = CheckoutSessionRepository(
            fraudDetectionDataRepository = FakeFraudDetectionDataRepository.create(),
            stripeNetworkClient = DefaultStripeNetworkClient(),
            analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                publishableKey = "pk_test_123",
            ),
            apiRequestOptionsProvider = {
                ApiRequest.Options(
                    apiKey = DEFAULT_API_CONFIG.publishableKey,
                    stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
                )
            },
        )

        return CheckoutSessionTaxRegionUpdater(checkoutSessionRepository)
    }

    private data class Scenario(
        val continueCoordinator: DefaultSheetActivityContinueCoordinator,
        val stateHolder: FakeSheetActivityStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
        val customerStateHolder: FakeCustomerStateHolder,
        val testScope: TestScope,
        val navigator: EmbeddedNavigator,
        val eventReporter: FakeEventReporter,
    ) {
        fun completeResult(
            selection: PaymentSelection? = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            checkoutSessionResponse: CheckoutSessionResponse?,
        ): EmbeddedActivityResult.Complete {
            return EmbeddedActivityResult.Complete(
                selection = selection,
                previousNewSelections = selectionHolder.previousNewSelections,
                hasBeenConfirmed = false,
                customerState = customerStateHolder.customer.value,
                linkAccountInfo = LinkAccountUpdate.Value(null),
                checkoutSessionResponse = checkoutSessionResponse,
                shouldInvokeSelectionCallback = false,
                launchMode = LAUNCH_MODE,
            )
        }
    }

    private companion object {
        const val ERROR_MESSAGE = "Tax region update failed"
        val LAUNCH_MODE = EmbeddedLaunchMode.PaymentOptions
        val CHECKOUT_SESSION_METADATA = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                collectedEmail = null,
                id = "cs_test_123",
                instancesKey = "test_instances_key",
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    automaticTaxEnabled = true,
                    taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                ),
            )
        )
    }
}
