@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutConfirmationContext.InvocationSource
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeAnalyticsRequestExecutor
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkDisabledState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@OptIn(ApiConfigurationPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsPerformerTest {
    @Test
    fun `payment element confirmation reports success only once`() = runScenario {
        startReporting()
        startConfirmation(InvocationSource.Merchant, PaymentSelection.GooglePay, confirmParams())

        val success = successfulResult()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            confirmParams(),
            duration = CONFIRMATION_DURATION_SECONDS,
        )

        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
    }

    @Test
    fun `confirmation duration is measured from confirmation start to its result`() = runScenario {
        startReporting()
        startConfirmation(InvocationSource.Merchant, PaymentSelection.GooglePay, confirmParams())
        assertThat(durationProvider.callsTurbine.awaitItem())
            .isEqualTo(FakeDurationProvider.Call.Start(DurationProvider.Key.CheckoutConfirmation, reset = true))

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        assertThat(durationProvider.callsTurbine.awaitItem())
            .isEqualTo(FakeDurationProvider.Call.End(DurationProvider.Key.CheckoutConfirmation))
        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            confirmParams(),
            duration = CONFIRMATION_DURATION_SECONDS,
        )
        durationProvider.validate()
    }

    @Test
    fun `payment failure reports safe fields`() = runScenario {
        val expectedParams = confirmParams(invocationSource = "express_checkout_element")
        startReporting()
        startConfirmation(InvocationSource.ExpressCheckoutElement, PaymentSelection.GooglePay, expectedParams)
        val privateMessage = "private payment details"
        val error = APIException(
            stripeError = StripeError(
                type = "card_error",
                code = "card_declined",
                message = privateMessage,
            ),
            requestId = "req_failed_confirmation",
            message = privateMessage,
        )
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = error,
                message = privateMessage.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
            )
        )

        val request = analyticsRequestExecutor.awaitConfirmationEvent(
            FAILED_EVENT,
            expectedParams + mapOf(
                "error_type" to "card_error",
                "error_code" to "card_declined",
                "request_id" to "req_failed_confirmation",
            ),
            duration = CONFIRMATION_DURATION_SECONDS,
        )
        assertThat(request.toString()).doesNotContain(privateMessage)
    }

    @Test
    fun `Transaction failure preserves its analytics error classification`() = assertFailureReported(
        type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(
            analyticsValue = "transaction_error",
            errorCode = "transaction_code",
        ),
        expectedErrorParams = mapOf(
            "error_type" to "transaction_error",
            "error_code" to "transaction_code",
        ),
    )

    @Test
    fun `internal failure reports a confirmation event`() = assertFailureReported(
        ConfirmationHandler.Result.Failed.ErrorType.Internal,
    )

    @Test
    fun `merchant integration failure reports a confirmation event`() = assertFailureReported(
        ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
    )

    @Test
    fun `fatal failure reports a confirmation event`() = assertFailureReported(
        ConfirmationHandler.Result.Failed.ErrorType.Fatal,
    )

    @Test
    fun `canceled confirmation reports canceled without error fields`() = runScenario {
        startReporting()
        startConfirmation(InvocationSource.Merchant, PaymentSelection.GooglePay, confirmParams())

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )

        analyticsRequestExecutor.awaitConfirmationEvent(
            CANCELED_EVENT,
            confirmParams(),
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `express checkout confirmation context survives performer recreation`() = runScenario {
        stateHolder.state = stateHolder.state!!.copy(
            expressCheckoutElementPaymentMethodMetadata = linkMetadata(LinkState.LoginState.NeedsVerification),
        )
        val expectedParams = confirmParams(
            selectedLpm = "link",
            invocationSource = "express_checkout_element",
            linkContext = "wallet",
            linkUi = "native-popup",
        )
        startConfirmation(
            InvocationSource.ExpressCheckoutElement,
            PaymentSelection.Link(brand = LinkBrand.Link),
            expectedParams,
        )

        val restoredPerformer = recreatePerformer()
        startReporting(restoredPerformer)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `native returning Link UI survives account changes and performer recreation`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedIn),
    ) {
        val expectedParams = confirmParams(
            selectedLpm = "link",
            linkContext = "wallet",
            linkUi = "native-returning",
            activeLinkSession = true,
        )
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        startConfirmation(InvocationSource.Merchant, nativeLinkSelection, expectedParams)
        linkAccountHolder.set(LinkAccountUpdate.Value(null))
        startReporting(recreatePerformer())

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("Unable to confirm"),
                message = "Unable to confirm".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
            )
        )

        analyticsRequestExecutor.awaitConfirmationEvent(
            FAILED_EVENT,
            expectedParams + mapOf("error_type" to "java.lang.IllegalStateException"),
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `native Link signup reports its UI`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedIn),
    ) {
        val expectedParams = confirmParams(
            selectedLpm = "link",
            linkContext = "wallet",
            linkUi = "native-signup",
            activeLinkSession = true,
        )
        val signupSession = TestFactory.CONSUMER_SESSION.copy(
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = ConsumerSession.VerificationSession.SessionType.SignUp,
                    state = ConsumerSession.VerificationSession.SessionState.Started,
                )
            ),
        )
        linkAccountHolder.set(LinkAccountUpdate.Value(LinkAccount(signupSession)))
        startReporting()
        startConfirmation(InvocationSource.Merchant, nativeLinkSelection, expectedParams)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `verified Link auth token without OTP reports native signup UI`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedIn),
    ) {
        val expectedParams = confirmParams(
            selectedLpm = "link",
            linkContext = "wallet",
            linkUi = "native-signup",
            activeLinkSession = true,
        )
        val verifiedSession = TestFactory.CONSUMER_SESSION.copy(
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = ConsumerSession.VerificationSession.SessionType.LinkAuthToken,
                    state = ConsumerSession.VerificationSession.SessionState.Verified,
                )
            ),
            currentAuthenticationLevel = null,
            minimumAuthenticationLevel = null,
        )
        linkAccountHolder.set(LinkAccountUpdate.Value(LinkAccount(verifiedSession)))
        startReporting()
        startConfirmation(InvocationSource.Merchant, nativeLinkSelection, expectedParams)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `verified Email OTP reports native returning UI`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedIn),
    ) {
        val expectedParams = confirmParams(
            selectedLpm = "link",
            linkContext = "wallet",
            linkUi = "native-returning",
            activeLinkSession = true,
        )
        val verifiedEmailSession = TestFactory.CONSUMER_SESSION.copy(
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = ConsumerSession.VerificationSession.SessionType.Email,
                    state = ConsumerSession.VerificationSession.SessionState.Verified,
                )
            ),
        )
        linkAccountHolder.set(LinkAccountUpdate.Value(LinkAccount(verifiedEmailSession)))
        startReporting()
        startConfirmation(InvocationSource.Merchant, nativeLinkSelection, expectedParams)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `signup with verified SMS OTP reports native returning UI`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedIn),
    ) {
        val expectedParams = confirmParams(
            selectedLpm = "link",
            linkContext = "wallet",
            linkUi = "native-returning",
            activeLinkSession = true,
        )
        val verifiedSignupSession = TestFactory.CONSUMER_SESSION.copy(
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = ConsumerSession.VerificationSession.SessionType.SignUp,
                    state = ConsumerSession.VerificationSession.SessionState.Started,
                ),
                ConsumerSession.VerificationSession(
                    type = ConsumerSession.VerificationSession.SessionType.Sms,
                    state = ConsumerSession.VerificationSession.SessionState.Verified,
                ),
            ),
        )
        linkAccountHolder.set(LinkAccountUpdate.Value(LinkAccount(verifiedSignupSession)))
        startReporting()
        startConfirmation(InvocationSource.Merchant, nativeLinkSelection, expectedParams)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `new card inline Link signup reports its UI`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedOut),
    ) {
        val expectedParams = confirmParams(selectedLpm = "card", linkUi = "inline-signup")
        startReporting()
        startConfirmation(InvocationSource.Merchant, inlineSignupSelection, expectedParams)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `saved card inline Link signup reports its UI`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedOut),
    ) {
        val expectedParams = confirmParams(selectedLpm = "card", isSavedPaymentMethod = true, linkUi = "inline-signup")
        startReporting()
        val selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            linkInput = inlineSignupSelection.linkInput,
        )
        startConfirmation(InvocationSource.Merchant, selection, expectedParams)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `inline Link sign in does not report signup UI`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.NeedsVerification),
    ) {
        val expectedParams = confirmParams(selectedLpm = "card")
        startReporting()
        val selection = inlineSignupSelection.copy(linkInput = UserInput.SignIn("email@example.com"))
        startConfirmation(InvocationSource.Merchant, selection, expectedParams)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `express checkout uses its Link session when Payment Element Link is disabled`() = runScenario {
        stateHolder.state = stateHolder.state!!.copy(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                apiConfiguration = ApiConfiguration(DEFAULT_METADATA_PUBLISHABLE_KEY).build(),
                linkConfiguration = PaymentSheet.LinkConfiguration(
                    display = PaymentSheet.LinkConfiguration.Display.Never,
                ),
                linkState = LinkDisabledState(listOf(LinkDisabledReason.LinkConfiguration)),
            ),
            expressCheckoutElementPaymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedIn),
        )
        val expectedParams = confirmParams(
            selectedLpm = "link",
            invocationSource = "express_checkout_element",
            linkContext = "wallet",
            linkUi = "native-popup",
            activeLinkSession = true,
        )
        startReporting()
        startConfirmation(
            InvocationSource.ExpressCheckoutElement,
            PaymentSelection.Link(brand = LinkBrand.Link),
            expectedParams,
        )

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `saved payment method confirmation reports the saved selection`() = runScenario {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val expectedParams = confirmParams(selectedLpm = "card", isSavedPaymentMethod = true)
        stateHolder.state = stateHolder.state!!.copy(paymentSelection = selection)
        startReporting()
        startConfirmation(InvocationSource.Merchant, selection, expectedParams)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `verified Link account takes precedence over loaded login state`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.NeedsVerification),
    ) {
        val expectedParams = confirmParams(activeLinkSession = true)
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        startReporting()
        startConfirmation(InvocationSource.Merchant, PaymentSelection.GooglePay, expectedParams)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            expectedParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `confirmation response provides completed session fields`() = runScenario {
        val elementsSession = ElementsSession.createFromFallback(
            stripeIntent = requireNotNull(stateHolder.state?.paymentMethodMetadata).stripeIntent,
            sessionsError = null,
        ).copy(elementsSessionConfigId = "cfg_elements_initial")
        val initialResponse = CheckoutSessionResponseFactory.create(
            configId = "cfg_initial_confirmation",
            elementsSession = elementsSession,
            automaticTaxEnabled = true,
        )
        stateHolder.state = stateHolder.state!!.copy(checkoutSessionResponse = initialResponse)
        startReporting()
        startConfirmation(
            InvocationSource.Merchant,
            PaymentSelection.GooglePay,
            confirmParams() + mapOf(
                "checkout_config_id" to "cfg_initial_confirmation",
                "elements_session_config_id" to "cfg_elements_initial",
                "automatic_tax_enabled" to true,
            ),
        )
        val confirmationResponse = initialResponse.copy(
            status = CheckoutSessionResponse.Status.COMPLETE,
            paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            configId = "cfg_confirmed",
            elementsSession = elementsSession.copy(
                stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
                elementsSessionConfigId = "cfg_elements_confirmed",
            ),
        )
        val resultWithResponse = successfulResult().copy(
            metadata = MutableConfirmationMetadata().apply {
                set(CheckoutSessionResponseKey, confirmationResponse)
            }
        )

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(resultWithResponse)

        analyticsRequestExecutor.awaitConfirmationEvent(
            SUCCEEDED_EVENT,
            confirmParams() + mapOf(
                "checkout_session_id" to initialResponse.id,
                "session_status" to "complete",
                "payment_status" to "paid",
                "checkout_config_id" to "cfg_confirmed",
                "elements_session_config_id" to "cfg_elements_confirmed",
                "currency" to "usd",
                "automatic_tax_enabled" to true,
            ),
            duration = CONFIRMATION_DURATION_SECONDS,
        )
    }

    @Test
    fun `init uses the active API configuration instead of previously committed payment metadata`() = runScenario {
        performer.createInitAttempt("cs_test_newinit").started(response = null)

        val request = analyticsRequestExecutor.awaitInitEvent(INIT_STARTED_EVENT)
        assertThat(request).containsEntry("checkout_session_id", "cs_test_newinit")
        assertThat(request).doesNotContainKey("checkout_config_id")
    }

    @Test
    fun `queued init attempt includes its wait in its own duration`() = runScenario {
        val first = performer.createInitAttempt("cs_test_first")
        first.started(response = null)
        val firstStarted = analyticsRequestExecutor.awaitInitEvent(INIT_STARTED_EVENT)
        assertThat(firstStarted).containsEntry("checkout_session_id", "cs_test_first")
        assertThat(firstStarted).doesNotContainKey("duration")

        timeSource += 150.milliseconds
        val second = performer.createInitAttempt("cs_test_second")
        second.started(response = null)
        val secondStarted = analyticsRequestExecutor.awaitInitEvent(INIT_STARTED_EVENT)
        assertThat(secondStarted).containsEntry("checkout_session_id", "cs_test_second")

        timeSource += 350.milliseconds
        first.succeeded(
            response = CheckoutSessionResponseFactory.create(id = "cs_test_first"),
            orderedLpms = listOf("card", "klarna"),
        )
        val succeeded = analyticsRequestExecutor.awaitInitEvent(INIT_SUCCEEDED_EVENT)
        assertThat(succeeded).containsEntry("checkout_session_id", "cs_test_first")
        assertThat(succeeded).containsEntry("ordered_lpms", "card,klarna")
        assertThat(succeeded).containsEntry("duration", 0.5f)

        timeSource += 500.milliseconds
        second.failed(response = null, error = IllegalStateException("private failure details"))
        val failed = analyticsRequestExecutor.awaitInitEvent(INIT_FAILED_EVENT)
        assertThat(failed).containsEntry("checkout_session_id", "cs_test_second")
        assertThat(failed).containsEntry("duration", 0.85f)
        assertThat(failed).containsEntry("error_type", "java.lang.IllegalStateException")
        assertThat(failed).doesNotContainKey("checkout_config_id")
        assertThat(failed.toString()).doesNotContain("private failure details")
    }

    private fun assertFailureReported(
        type: ConfirmationHandler.Result.Failed.ErrorType,
        expectedErrorParams: Map<String, Any> = mapOf("error_type" to "java.lang.IllegalStateException"),
    ) = runScenario {
        startReporting()
        startConfirmation(InvocationSource.Merchant, PaymentSelection.GooglePay, confirmParams())

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("private failure details"),
                message = "Unable to confirm".resolvableString,
                type = type,
            )
        )

        val request = analyticsRequestExecutor.awaitConfirmationEvent(
            FAILED_EVENT,
            confirmParams() + expectedErrorParams,
            duration = CONFIRMATION_DURATION_SECONDS,
        )
        assertThat(request.toString()).doesNotContain("private failure details")
    }

    private fun linkMetadata(loginState: LinkState.LoginState): PaymentMethodMetadata =
        PaymentMethodMetadataFactory.create(
            apiConfiguration = ApiConfiguration(DEFAULT_METADATA_PUBLISHABLE_KEY).build(),
            linkState = LinkState(
                configuration = LinkTestUtils.createLinkConfiguration(),
                loginState = loginState,
                signupMode = null,
            )
        )

    private suspend fun Scenario.startConfirmation(
        invocationSource: InvocationSource,
        paymentSelection: PaymentSelection,
        expectedParams: Map<String, Any>,
    ) {
        when (invocationSource) {
            InvocationSource.Merchant ->
                performer.onPaymentElementConfirmationStarted(paymentSelection)
            InvocationSource.ExpressCheckoutElement ->
                performer.onExpressCheckoutElementConfirmationStarted(paymentSelection)
        }
        analyticsRequestExecutor.awaitConfirmationEvent(STARTED_EVENT, expectedParams)
    }

    /**
     * Asserts every confirm-event key from the Metrics spec exactly, so missing or unexpected spec keys fail.
     * [params] entries override the default [sessionParams].
     */
    private suspend fun FakeAnalyticsRequestExecutor.awaitConfirmationEvent(
        event: String,
        params: Map<String, Any>,
        duration: Float? = null,
    ): Map<String, *> {
        val request = awaitEvent(event)
        assertThat(request).containsEntry("product_usage", "checkout_elements")
        assertThat(request).containsEntry("publishable_key", DEFAULT_METADATA_PUBLISHABLE_KEY)
        val expected = sessionParams + params + listOfNotNull(duration?.let { "duration" to it })
        assertThat(request.filterKeys { it in confirmationSpecKeys }).containsExactlyEntriesIn(expected)
        return request
    }

    private suspend fun FakeAnalyticsRequestExecutor.awaitInitEvent(event: String): Map<String, *> {
        val request = awaitEvent(event)
        assertThat(request).containsEntry("product_usage", "checkout_elements")
        assertThat(request).containsEntry("publishable_key", FALLBACK_PUBLISHABLE_KEY)
        return request
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            apiConfiguration = ApiConfiguration(DEFAULT_METADATA_PUBLISHABLE_KEY).build(),
        ),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle).apply {
            state = CheckoutControllerStateFactory.create(
                paymentSelection = PaymentSelection.GooglePay,
                paymentMethodMetadata = paymentMethodMetadata,
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    configId = "cfg_checkout",
                    elementsSession = ElementsSession.createFromFallback(
                        stripeIntent = paymentMethodMetadata.stripeIntent,
                        sessionsError = null,
                    ).copy(elementsSessionConfigId = "cfg_elements"),
                ),
            )
        }
        val linkAccountHolder = LinkAccountHolder(savedStateHandle)
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val durationProvider = FakeDurationProvider(CONFIRMATION_DURATION)
        val scenario = Scenario(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            linkAccountHolder = linkAccountHolder,
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            fallbackApiConfiguration = ApiConfiguration(FALLBACK_PUBLISHABLE_KEY).build(),
            timeSource = TestTimeSource(),
            backgroundScope = backgroundScope,
            testScheduler = testScheduler,
        )

        scenario.block()

        confirmationHandler.validate()
        analyticsRequestExecutor.validate()
    }

    private class Scenario(
        val confirmationHandler: FakeConfirmationHandler,
        private val savedStateHandle: SavedStateHandle,
        val stateHolder: CheckoutControllerStateHolder,
        val linkAccountHolder: LinkAccountHolder,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val durationProvider: FakeDurationProvider,
        private val fallbackApiConfiguration: ApiConfiguration.State,
        val timeSource: TestTimeSource,
        private val backgroundScope: CoroutineScope,
        private val testScheduler: TestCoroutineScheduler,
    ) {
        val performer: CheckoutAnalyticsPerformer = createPerformer()

        fun recreatePerformer(): CheckoutAnalyticsPerformer = createPerformer()

        fun startReporting(performer: CheckoutAnalyticsPerformer = this.performer) {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                performer.reportConfirmationResults()
            }
        }

        private fun createPerformer(): CheckoutAnalyticsPerformer = TestCheckoutAnalyticsPerformerFactory.create(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            linkAccountHolder = linkAccountHolder,
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            apiConfiguration = fallbackApiConfiguration,
            timeSource = timeSource,
            workContext = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private companion object {
        const val FALLBACK_PUBLISHABLE_KEY = "pk_test_checkout_analytics"
        const val DEFAULT_METADATA_PUBLISHABLE_KEY = "pk_test_checkout_metadata"

        const val STARTED_EVENT = "checkout_elements.confirm.started"
        const val SUCCEEDED_EVENT = "checkout_elements.confirm.succeeded"
        const val FAILED_EVENT = "checkout_elements.confirm.failed"
        const val CANCELED_EVENT = "checkout_elements.confirm.canceled"

        const val INIT_STARTED_EVENT = "checkout_elements.init.started"
        const val INIT_SUCCEEDED_EVENT = "checkout_elements.init.succeeded"
        const val INIT_FAILED_EVENT = "checkout_elements.init.failed"

        val CONFIRMATION_DURATION = 2.seconds
        const val CONFIRMATION_DURATION_SECONDS = 2f

        val sessionParams: Map<String, Any> = mapOf(
            "checkout_session_id" to DEFAULT_CHECKOUT_SESSION_ID,
            "checkout_config_id" to "cfg_checkout",
            "elements_session_config_id" to "cfg_elements",
            "currency" to "usd",
            "session_status" to "open",
            "payment_status" to "unpaid",
            "adaptive_pricing_active" to false,
            "automatic_tax_enabled" to false,
        )

        val confirmationSpecKeys: Set<String> = sessionParams.keys + setOf(
            "selected_lpm",
            "is_saved_payment_method",
            "link_context",
            "link_ui",
            "active_link_session",
            "invocation_source",
            "duration",
            "error_type",
            "error_code",
            "request_id",
        )

        fun confirmParams(
            selectedLpm: String = "google_pay",
            invocationSource: String = "merchant",
            isSavedPaymentMethod: Boolean = false,
            linkContext: String? = null,
            linkUi: String? = null,
            activeLinkSession: Boolean = false,
        ): Map<String, Any> = buildMap {
            put("selected_lpm", selectedLpm)
            put("is_saved_payment_method", isSavedPaymentMethod)
            linkContext?.let { put("link_context", it) }
            linkUi?.let { put("link_ui", it) }
            put("active_link_session", activeLinkSession)
            put("invocation_source", invocationSource)
        }

        fun successfulResult() = ConfirmationHandler.Result.Succeeded(
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            metadata = MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Client)
            },
        )

        val nativeLinkSelection = PaymentSelection.Link(
            brand = LinkBrand.Link,
            selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = null,
                billingPhone = null,
            ),
        )
        val inlineSignupSelection = PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            linkInput = UserInput.SignUp(
                email = "email@example.com",
                phone = "1234567890",
                country = "US",
                name = null,
                consentAction = SignUpConsentAction.Checkbox,
            ),
        )
    }
}
