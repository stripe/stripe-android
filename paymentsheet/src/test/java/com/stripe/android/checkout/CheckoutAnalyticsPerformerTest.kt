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
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.model.PaymentIntentFixtures
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
import kotlin.time.Duration.Companion.seconds

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
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
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
    fun `Google Pay failure preserves its analytics error classification`() = assertFailureReported(
        type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(errorCode = 12),
        expectedErrorParams = mapOf("error_type" to "googlePay_12", "error_code" to "12"),
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
    fun `express checkout confirmation context survives process death without duration`() = runScenario {
        val expectedParams = confirmParams(
            selectedLpm = "link",
            invocationSource = "express_checkout_element",
            linkContext = "wallet",
        )
        startConfirmation(
            InvocationSource.ExpressCheckoutElement,
            PaymentSelection.Link(brand = LinkBrand.Link),
            expectedParams,
        )

        val restoredPerformer = recreatePerformerAfterProcessDeath()
        startReporting(restoredPerformer)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        // The confirmation start time is held in memory, so the restored event has no duration.
        analyticsRequestExecutor.awaitConfirmationEvent(SUCCEEDED_EVENT, expectedParams)
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
        private val backgroundScope: CoroutineScope,
        private val testScheduler: TestCoroutineScheduler,
    ) {
        val performer: CheckoutAnalyticsPerformer = createPerformer(durationProvider)

        /** The new process keeps [savedStateHandle] but gets a duration provider without the start. */
        fun recreatePerformerAfterProcessDeath(): CheckoutAnalyticsPerformer =
            createPerformer(FakeDurationProvider(duration = null))

        fun startReporting(performer: CheckoutAnalyticsPerformer = this.performer) {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                performer.reportConfirmationResults()
            }
        }

        private fun createPerformer(
            durationProvider: DurationProvider,
        ): CheckoutAnalyticsPerformer = TestCheckoutAnalyticsPerformerFactory.create(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            linkAccountHolder = linkAccountHolder,
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            apiConfiguration = fallbackApiConfiguration,
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
            activeLinkSession: Boolean = false,
        ): Map<String, Any> = buildMap {
            put("selected_lpm", selectedLpm)
            put("is_saved_payment_method", isSavedPaymentMethod)
            linkContext?.let { put("link_context", it) }
            put("active_link_session", activeLinkSession)
            put("invocation_source", invocationSource)
        }

        fun successfulResult() = ConfirmationHandler.Result.Succeeded(
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            metadata = MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Client)
            },
        )
    }
}
