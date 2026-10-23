@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ApiConfigurationPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsPerformerTest {
    @Test
    fun `payment element confirmation reports success only once`() = runScenario {
        val selection = PaymentSelection.GooglePay
        performer.onPaymentElementConfirmationStarted(selection)

        analyticsRequestExecutor.requestTurbine.expectNoEvents()

        startReporting()

        val success = successfulResult()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf(
                    "selected_lpm" to "google_pay",
                    "duration" to 2f,
                ),
            ),
        )

        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
    }

    @Test
    fun `payment failure reports safe fields`() = runScenario {
        startReporting()
        performer.onExpressCheckoutElementConfirmationStarted(PaymentSelection.GooglePay)
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

        val request = analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.failed",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf(
                    "selected_lpm" to "google_pay",
                    "duration" to 2f,
                    "error_type" to "card_error",
                    "error_code" to "card_declined",
                ),
            ),
        )
        assertThat(request).containsEntry("request_id", "req_failed_confirmation")
        assertThat(request.toString()).doesNotContain(privateMessage)
    }

    @Test
    fun `Google Pay failure preserves its analytics error classification`() = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        val privateMessage = "private Google Pay details"
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException(privateMessage),
                message = privateMessage.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(errorCode = 12),
            )
        )

        val request = analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.failed",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf(
                    "selected_lpm" to "google_pay",
                    "error_type" to "googlePay_12",
                    "error_code" to "12",
                ),
            ),
        )
        assertThat(request.toString()).doesNotContain(privateMessage)
    }

    @Test
    fun `internal failure is filtered from confirmation analytics`() = assertFailureNotReported(
        ConfirmationHandler.Result.Failed.ErrorType.Internal,
    )

    @Test
    fun `merchant integration failure is filtered from confirmation analytics`() = assertFailureNotReported(
        ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
    )

    @Test
    fun `fatal failure is filtered from confirmation analytics`() = assertFailureNotReported(
        ConfirmationHandler.Result.Failed.ErrorType.Fatal,
    )

    @Test
    fun `canceled confirmation does not report a terminal event`() = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        savedStateHandle.checkoutConfirmationRequestId = "req_before_cancellation"
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
            )
        )

        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout))).isFalse()
        assertThat(savedStateHandle.checkoutConfirmationRequestId).isNull()
    }

    @Test
    fun `express checkout context survives recreation and reports terminal request id`() = runScenario {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)
        performer.onExpressCheckoutElementConfirmationStarted(selection)

        savedStateHandle.checkoutConfirmationRequestId = "req_before_next_action"
        val restoredPerformer = recreatePerformer()
        startReporting(restoredPerformer)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf(
                    "selected_lpm" to "link",
                    "request_id" to "req_before_next_action",
                ),
            ),
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
        startReporting()
        performer.onExpressCheckoutElementConfirmationStarted(PaymentSelection.Link(brand = LinkBrand.Link))

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = expressCheckoutConfirmationParams + ("active_link_session" to true),
            ),
        )
    }

    @Test
    fun `saved payment method confirmation reports the saved selection`() = runScenario {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        stateHolder.state = stateHolder.state!!.copy(paymentSelection = selection)
        startReporting()
        performer.onPaymentElementConfirmationStarted(selection)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf(
                    "selected_lpm" to "card",
                    "is_saved_payment_method" to true,
                ),
            ),
        )
    }

    @Test
    fun `verified Link account takes precedence over loaded login state`() = runScenario(
        paymentMethodMetadata = linkMetadata(LinkState.LoginState.NeedsVerification),
    ) {
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf("active_link_session" to true),
            ),
        )
    }

    @Test
    fun `confirmation response provides completed session fields`() = runScenario {
        val elementsSession = ElementsSession.createFromFallback(
            stripeIntent = requireNotNull(stateHolder.state).paymentMethodMetadata.stripeIntent,
            sessionsError = null,
        ).copy(elementsSessionConfigId = "cfg_elements_initial")
        val initialResponse = CheckoutSessionResponseFactory.create(
            configId = "cfg_initial_confirmation",
            elementsSession = elementsSession,
            automaticTaxEnabled = true,
        )
        stateHolder.state = stateHolder.state!!.copy(checkoutSessionResponse = initialResponse)
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        val confirmationResponse = initialResponse.copy(
            status = CheckoutSessionResponse.Status.COMPLETE,
            paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            configId = "cfg_confirmed",
            elementsSession = elementsSession.copy(
                stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
                elementsSessionConfigId = "cfg_elements_confirmed",
            ),
        )
        savedStateHandle.checkoutConfirmationRequestId = "req_confirmed"
        val resultWithResponse = successfulResult().copy(
            metadata = MutableConfirmationMetadata().apply {
                set(CheckoutSessionResponseKey, confirmationResponse)
            }
        )
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(resultWithResponse)

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = paymentElementConfirmationParams + mapOf(
                    "checkout_session_id" to initialResponse.id,
                    "session_status" to "complete",
                    "payment_status" to "paid",
                    "checkout_config_id" to "cfg_confirmed",
                    "request_id" to "req_confirmed",
                    "elements_session_config_id" to "cfg_elements_confirmed",
                    "currency" to "usd",
                    "automatic_tax_enabled" to true,
                ),
            ),
        )
    }

    @Test
    fun `success without a confirm response id omits request id`() = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            successfulResult().copy(
                metadata = MutableConfirmationMetadata().apply {
                    set(
                        CheckoutSessionResponseKey,
                        CheckoutSessionResponseFactory.create(
                            status = CheckoutSessionResponse.Status.COMPLETE,
                            paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
                        ),
                    )
                }
            )
        )

        val request = analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = paymentElementConfirmationParams,
            ),
        )
        assertThat(request).doesNotContainKey("request_id")
    }

    @Test
    fun `next action failure retains confirm request id`() = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        savedStateHandle.checkoutConfirmationRequestId = "req_confirm_before_next_action"
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("Authentication failed"),
                message = "Unable to confirm".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.failed",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf("request_id" to "req_confirm_before_next_action"),
            ),
        )
    }

    @Test
    fun `confirm request id takes precedence over next action error request id`() = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        savedStateHandle.checkoutConfirmationRequestId = "req_confirm_before_next_action"
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = APIException(requestId = "req_next_action"),
                message = "Unable to confirm".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.failed",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf("request_id" to "req_confirm_before_next_action"),
            ),
        )
    }

    @Test
    fun `new confirmation does not reuse the previous request id`() = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        savedStateHandle.checkoutConfirmationRequestId = "req_first_confirm"
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())
        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf("request_id" to "req_first_confirm"),
            ),
        )
        assertThat(hasSavedConfirmationContext()).isFalse()
        assertThat(savedStateHandle.checkoutConfirmationRequestId).isNull()

        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = APIException(requestId = "req_second_confirm"),
                message = "Unable to confirm".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.failed",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf("request_id" to "req_second_confirm"),
            ),
        )
    }

    private fun assertFailureNotReported(type: ConfirmationHandler.Result.Failed.ErrorType) = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("private failure details"),
                message = "Unable to confirm".resolvableString,
                type = type,
            )
        )

        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout))).isFalse()
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

    private fun runScenario(
        duration: Duration = 2.seconds,
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
            )
        }
        val linkAccountHolder = LinkAccountHolder(savedStateHandle)
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val durationProvider = FakeDurationProvider(duration)
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
        val savedStateHandle: SavedStateHandle,
        val stateHolder: CheckoutControllerStateHolder,
        val linkAccountHolder: LinkAccountHolder,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val durationProvider: FakeDurationProvider,
        private val fallbackApiConfiguration: ApiConfiguration.State,
        private val backgroundScope: CoroutineScope,
        private val testScheduler: TestCoroutineScheduler,
    ) {
        val performer: CheckoutAnalyticsPerformer = createPerformer()

        fun recreatePerformer(): CheckoutAnalyticsPerformer = createPerformer()

        fun hasSavedConfirmationContext(): Boolean =
            savedStateHandle.contains("CheckoutAnalyticsPerformer.confirmationContext")

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
            workContext = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private data class ExpectedEvent(
        val event: String,
        val publishableKey: String,
        val params: Map<String, Any?>,
    )

    private suspend fun FakeAnalyticsRequestExecutor.awaitAndAssertConfirmationRequest(
        expected: ExpectedEvent,
    ): Map<String, *> {
        val request = requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", expected.event)
        assertThat(request.params).containsEntry("product_usage", "checkout_elements")
        assertThat(request.params).containsEntry("publishable_key", expected.publishableKey)
        expected.params.forEach { (key, value) ->
            assertThat(request.params).containsEntry(key, value)
        }
        return request.params
    }

    private companion object {
        const val FALLBACK_PUBLISHABLE_KEY = "pk_test_checkout_analytics"
        const val DEFAULT_METADATA_PUBLISHABLE_KEY = "pk_test_checkout_metadata"

        fun successfulResult() = ConfirmationHandler.Result.Succeeded(
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            metadata = MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Client)
            },
        )

        val paymentElementConfirmationParams = mapOf(
            "selected_lpm" to "google_pay",
            "is_saved_payment_method" to false,
            "invocation_source" to "merchant",
        )
        val expressCheckoutConfirmationParams = mapOf(
            "selected_lpm" to "link",
            "is_saved_payment_method" to false,
            "link_context" to "wallet",
            "invocation_source" to "express_checkout_element",
        )
    }
}
