package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.sheet.SheetTaxRegionUpdater
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutSheetLinkHelperTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val paymentConfigurationTestRule = PaymentConfigurationTestRule(applicationContext)

    @Test
    fun `launchLinkIfEligible launches Link when selected and RUX is enabled`() = testScenario(
        initialState = linkCheckoutState(),
    ) {
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        val didLaunch = launchLink()

        assertThat(didLaunch).isTrue()
        val call = linkLauncherScenario.presentCalls.awaitItem()
        assertThat(call.configuration).isEqualTo(TestFactory.LINK_CONFIGURATION)
        assertThat(call.paymentMethodMetadata).isEqualTo(checkoutControllerStateHolder.state?.paymentMethodMetadata)
        assertThat(call.linkAccount).isEqualTo(TestFactory.LINK_ACCOUNT)
        assertThat(call.linkExpressMode).isEqualTo(LinkExpressMode.ENABLED)
        assertThat(call.launchMode).isEqualTo(
            LinkLaunchMode.PaymentMethodSelection(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)
        )
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `launchLinkIfEligible returns false when Link RUX is disabled`() = testScenario(
        initialState = linkCheckoutState(),
    ) {
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        linkGate.setShowRuxInFlowController(false)

        val didLaunch = launchLink()

        assertThat(didLaunch).isFalse()
        linkLauncherScenario.presentCalls.expectNoEvents()
    }

    @Test
    fun `completed Link result updates selection and account state`() = testScenario(
        initialState = linkCheckoutState(loginState = LinkState.LoginState.LoggedOut),
    ) {
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        launchLink()
        linkLauncherScenario.presentCalls.awaitItem()
        val updatedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(id = "csmrpd_updated"),
            collectedCvc = null,
            billingPhone = null,
        )

        linkResultCallback(
            LinkActivityResult.Completed(
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                selectedPayment = updatedPayment,
            )
        )

        val selection = selectionHolder.selection.value as PaymentSelection.Link
        assertThat(selection.selectedPayment).isEqualTo(updatedPayment)
        val linkState = checkoutControllerStateHolder.state?.paymentMethodMetadata?.linkState
        assertThat(linkState?.loginState).isEqualTo(LinkState.LoginState.LoggedIn)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `back from Link verification prevents eager relaunch`() = testScenario(
        initialState = linkCheckoutState(),
    ) {
        val verificationAccount = TestFactory.LINK_ACCOUNT.copy(
            consumerSession = TestFactory.CONSUMER_SESSION.copy(
                verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION),
                currentAuthenticationLevel = ConsumerSession.AuthenticationLevel.NotAuthenticated,
                minimumAuthenticationLevel = ConsumerSession.AuthenticationLevel.OneFactorAuthentication,
            )
        )
        linkAccountHolder.set(LinkAccountUpdate.Value(verificationAccount))
        launchLink()
        linkLauncherScenario.presentCalls.awaitItem()

        linkResultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.Value(verificationAccount),
            )
        )

        assertThat(sheetStateHolder.declinedLink2FA).isTrue()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(launchLink()).isFalse()
        linkLauncherScenario.presentCalls.expectNoEvents()
    }

    @Test
    fun `pay another way from Link launches current payment options`() = testScenario(
        initialState = linkCheckoutState(),
    ) {
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        launchLink()
        linkLauncherScenario.presentCalls.awaitItem()

        linkResultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.PayAnotherWay,
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            )
        )

        val call = launchPaymentOptionsCalls.awaitItem()
        assertThat(call.selection).isEqualTo(selectionHolder.selection.value)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `logout from Link selects saved fallback and launches payment options`() = testScenario(
        initialState = linkCheckoutState(hasCustomerConfiguration = true),
    ) {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(paymentMethod)))
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        launchLink()
        linkLauncherScenario.presentCalls.awaitItem()

        linkResultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.Value(null),
            )
        )

        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.Saved(paymentMethod))
        assertThat(launchPaymentOptionsCalls.awaitItem().selection).isEqualTo(PaymentSelection.Saved(paymentMethod))
    }

    @Test
    fun `completed Link result updates tax region before closing`() {
        val updatedResponse = CheckoutSessionResponseFactory.create(id = "cs_updated")
        val checkoutResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )
        testScenario(
            initialState = linkCheckoutState(checkoutSessionResponse = checkoutResponse),
            taxRegionUpdateResult = Result.success(updatedResponse),
        ) {
            sessionRefresher.enqueueRefreshAction {}
            linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
            launchLink()
            linkLauncherScenario.presentCalls.awaitItem()
            val updatedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = null,
                billingPhone = null,
            )

            linkResultCallback(
                LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                    selectedPayment = updatedPayment,
                )
            )
            runCurrent()

            assertThat(sessionRefresher.calls.awaitItem())
                .isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(updatedResponse))
            assertThat((selectionHolder.selection.value as PaymentSelection.Link).selectedPayment)
                .isEqualTo(updatedPayment)
            assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        }
    }

    @Test
    fun `failed Link tax update reopens payment options with updated selection`() {
        val error = IllegalStateException("Tax update failed")
        val checkoutResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )
        testScenario(
            initialState = linkCheckoutState(checkoutSessionResponse = checkoutResponse),
            taxRegionUpdateResult = Result.failure(error),
        ) {
            linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
            launchLink()
            linkLauncherScenario.presentCalls.awaitItem()
            val updatedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = null,
                billingPhone = null,
            )

            linkResultCallback(
                LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                    selectedPayment = updatedPayment,
                )
            )
            runCurrent()

            val call = launchPaymentOptionsCalls.awaitItem()
            assertThat((call.selection as PaymentSelection.Link).selectedPayment).isEqualTo(updatedPayment)
            assertThat(logger.errorLogs).containsExactly(
                "Failed to update the tax region after Link selection." to error
            )
            assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        }
    }

    @Test
    fun `restored pay another way result launches options from persisted state`() = testScenario(
        initialState = linkCheckoutState(),
    ) {
        sheetStateHolder.sheetIsOpen = true

        linkResultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.PayAnotherWay,
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            )
        )

        val call = launchPaymentOptionsCalls.awaitItem()
        assertThat(call.paymentMethodMetadata)
            .isEqualTo(checkoutControllerStateHolder.state?.paymentMethodMetadata)
        assertThat(call.configuration).isEqualTo(checkoutControllerStateHolder.state?.embeddedConfiguration)
    }

    @Suppress("LongMethod")
    private fun testScenario(
        initialState: CheckoutControllerState,
        taxRegionUpdateResult: Result<CheckoutSessionResponse>? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val testScope = this
        val savedStateHandle = SavedStateHandle()
        val errorReporter = FakeErrorReporter()
        val checkoutControllerStateHolder = CheckoutControllerStateFactory.createStateHolder(
            savedStateHandle = savedStateHandle,
            errorReporter = errorReporter,
        ).apply {
            state = initialState
        }
        val selectionHolder: EmbeddedSelectionHolder = checkoutControllerStateHolder
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(initialState.paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val sessionRefresher = FakeCheckoutSessionRefresher()
        val logger = FakeLogger()
        val linkAccountHolder = LinkAccountHolder(savedStateHandle)
        val linkGate = FakeLinkGate()
        val confirmationHandler = FakeConfirmationHandler()
        val operationCoordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = sheetStateHolder,
            sessionRefresher = sessionRefresher,
            logger = logger,
            resultCallback = CheckoutController.ResultCallback {},
        )
        val taxRegionUpdater = SheetTaxRegionUpdater { response, _, _ ->
            taxRegionUpdateResult ?: Result.success(response)
        }
        val launchPaymentOptionsCalls = Turbine<LaunchPaymentOptionsCall>()

        RecordingLinkPaymentLauncher.test {
            val linkLauncherScenario = this
            DummyActivityResultCaller.test {
                val helper = DefaultCheckoutSheetLinkHelper(
                    selectionHolder = selectionHolder,
                    customerStateHolder = customerStateHolder,
                    sheetStateHolder = sheetStateHolder,
                    errorReporter = errorReporter,
                    sessionRefresher = sessionRefresher,
                    operationCoordinator = operationCoordinator,
                    logger = logger,
                    checkoutControllerStateHolder = checkoutControllerStateHolder,
                    linkAccountHolder = linkAccountHolder,
                    linkGateFactory = FakeLinkGate.Factory(linkGate),
                    paymentElementLinkLauncher = linkLauncherScenario.launcher,
                    taxRegionUpdater = taxRegionUpdater,
                    coroutineScope = testScope,
                    statusBarColor = null,
                )
                helper.register(activityResultCaller) { metadata, customerState, selection, configuration ->
                    launchPaymentOptionsCalls.add(
                        LaunchPaymentOptionsCall(
                            paymentMethodMetadata = metadata,
                            customerState = customerState,
                            selection = selection,
                            configuration = configuration,
                        )
                    )
                }
                val linkRegisterCall = linkLauncherScenario.registerCalls.awaitItem()

                Scenario(
                    helper = helper,
                    selectionHolder = selectionHolder,
                    checkoutControllerStateHolder = checkoutControllerStateHolder,
                    customerStateHolder = customerStateHolder,
                    sheetStateHolder = sheetStateHolder,
                    sessionRefresher = sessionRefresher,
                    logger = logger,
                    linkAccountHolder = linkAccountHolder,
                    linkGate = linkGate,
                    linkLauncherScenario = linkLauncherScenario,
                    linkResultCallback = linkRegisterCall.callback,
                    launchPaymentOptionsCalls = launchPaymentOptionsCalls,
                    runCurrent = testScheduler::runCurrent,
                ).block()

                helper.unregister()
                linkLauncherScenario.unregisterCalls.awaitItem()
            }
        }

        launchPaymentOptionsCalls.ensureAllEventsConsumed()
        confirmationHandler.validate()
        sessionRefresher.ensureAllEventsConsumed()
    }

    private class Scenario(
        val helper: CheckoutSheetLinkHelper,
        val selectionHolder: EmbeddedSelectionHolder,
        val checkoutControllerStateHolder: CheckoutControllerStateHolder,
        val customerStateHolder: CustomerStateHolder,
        val sheetStateHolder: SheetStateHolder,
        val sessionRefresher: FakeCheckoutSessionRefresher,
        val logger: FakeLogger,
        val linkAccountHolder: LinkAccountHolder,
        val linkGate: FakeLinkGate,
        val linkLauncherScenario: RecordingLinkPaymentLauncher.Scenario,
        val linkResultCallback: (LinkActivityResult) -> Unit,
        val launchPaymentOptionsCalls: Turbine<LaunchPaymentOptionsCall>,
        private val runCurrent: () -> Unit,
    ) {
        fun launchLink(): Boolean {
            sheetStateHolder.sheetIsOpen = true
            val state = requireNotNull(checkoutControllerStateHolder.state)
            return helper.launchLinkIfEligible(
                paymentMethodMetadata = state.paymentMethodMetadata,
                selection = state.paymentSelection,
            )
        }

        fun runCurrent() {
            runCurrent.invoke()
        }
    }

    private data class LaunchPaymentOptionsCall(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val customerState: CustomerState?,
        val selection: PaymentSelection?,
        val configuration: EmbeddedPaymentElement.Configuration,
    )

    private companion object {
        val LINK_SELECTION = PaymentSelection.Link(
            brand = LinkBrand.Link,
            selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = null,
                billingPhone = null,
            ),
        )

        fun linkCheckoutState(
            loginState: LinkState.LoginState = LinkState.LoginState.LoggedIn,
            hasCustomerConfiguration: Boolean = false,
            checkoutSessionResponse: CheckoutSessionResponse? = null,
        ): CheckoutControllerState {
            val response = checkoutSessionResponse ?: CheckoutSessionResponseFactory.create()
            val integrationMetadata = checkoutSessionResponse?.let {
                IntegrationMetadata.CheckoutSession(
                    id = it.id,
                    instancesKey = "test_instances_key",
                    checkoutSessionResponse = it,
                )
            }
            return CheckoutControllerStateFactory.create(
                checkoutSessionResponse = response,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    hasCustomerConfiguration = hasCustomerConfiguration,
                    linkState = LinkState(
                        configuration = TestFactory.LINK_CONFIGURATION,
                        loginState = loginState,
                        signupMode = null,
                    ),
                    integrationMetadata = integrationMetadata
                        ?: PaymentMethodMetadataFactory.create().integrationMetadata,
                ),
                paymentSelection = LINK_SELECTION,
            )
        }
    }
}
