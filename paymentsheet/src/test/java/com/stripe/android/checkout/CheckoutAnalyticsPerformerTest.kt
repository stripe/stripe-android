@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.analytics.FakeAnalyticsRequestExecutor
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsPerformerTest {
    @Test
    fun `payment element confirmation reports success only once`() = runScenario {
        startReporting(performer)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        timeSource += 2.seconds

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        val events = events(count = 2)
        assertThat(events.map { it["event"] }).containsExactly(
            "checkout_elements.confirm.started",
            "checkout_elements.confirm.succeeded",
        ).inOrder()
        val publishableKey = requireNotNull(stateHolder.state)
            .paymentMethodMetadata.apiConfiguration.publishableKey
        assertEvent(
            events[0],
            ExpectedEvent(
                eventName = "checkout_elements.confirm.started",
                publishableKey = publishableKey,
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertEvent(
            events[1],
            ExpectedEvent(
                eventName = "checkout_elements.confirm.succeeded",
                publishableKey = publishableKey,
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertThat(events[0]).containsEntry("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(events[1]).containsEntry("duration", 2f)
        assertThat(events[1]).doesNotContainKey("request_id")

        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
    }

    @Test
    fun `queued events keep the publishable key captured from metadata before dispatch`() = runScenario(
        workContextFactory = { StandardTestDispatcher(it) },
    ) {
        val originalState = requireNotNull(stateHolder.state)
        val originalPublishableKey = originalState.paymentMethodMetadata.apiConfiguration.publishableKey

        startReporting(performer)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        stateHolder.state = originalState.copy(
            paymentMethodMetadata = originalState.paymentMethodMetadata.copy(
                apiConfiguration = originalState.paymentMethodMetadata.apiConfiguration.copy(
                    publishableKey = "pk_test_changed_after_enqueue",
                ),
            ),
        )
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        val events = events(count = 2)
        assertThat(events.map { it["event"] }).containsExactly(
            "checkout_elements.confirm.started",
            "checkout_elements.confirm.succeeded",
        ).inOrder()
        assertEvent(
            events[0],
            ExpectedEvent(
                eventName = "checkout_elements.confirm.started",
                publishableKey = originalPublishableKey,
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertEvent(
            events[1],
            ExpectedEvent(
                eventName = "checkout_elements.confirm.succeeded",
                publishableKey = "pk_test_changed_after_enqueue",
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
    }

    @Test
    fun `failed confirmation reports safe error fields`() = runScenario {
        startReporting(performer)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        timeSource += 2500.milliseconds
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

        val events = events(count = 2)
        assertEvent(
            events.first(),
            ExpectedEvent(
                eventName = "checkout_elements.confirm.started",
                publishableKey = currentPublishableKey(),
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        val params = events.last()
        assertEvent(
            params,
            ExpectedEvent(
                eventName = "checkout_elements.confirm.failed",
                publishableKey = currentPublishableKey(),
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertThat(params).containsEntry("error_type", "card_error")
        assertThat(params).containsEntry("error_code", "card_declined")
        assertThat(params).containsEntry("request_id", "req_failed_confirmation")
        assertThat(params).containsEntry("duration", 2.5f)
        assertThat(params.toString()).doesNotContain(privateMessage)
    }

    @Test
    fun `internal failure reports a terminal event`() = assertFailureReported(
        ConfirmationHandler.Result.Failed.ErrorType.Internal,
    )

    @Test
    fun `merchant integration failure reports a terminal event`() = assertFailureReported(
        ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
    )

    @Test
    fun `fatal failure reports a terminal event`() = assertFailureReported(
        ConfirmationHandler.Result.Failed.ErrorType.Fatal,
    )

    @Test
    fun `canceled confirmation reports canceled without error fields`() = runScenario {
        startReporting(performer)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        timeSource += 250.milliseconds

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )

        val events = events(count = 2)
        assertEvent(
            events.first(),
            ExpectedEvent(
                eventName = "checkout_elements.confirm.started",
                publishableKey = currentPublishableKey(),
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        val params = events.last()
        assertEvent(
            params,
            ExpectedEvent(
                eventName = "checkout_elements.confirm.canceled",
                publishableKey = currentPublishableKey(),
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertThat(params).containsEntry("duration", 0.25f)
        assertThat(params).doesNotContainKey("request_id")
        assertThat(params).doesNotContainKey("error_type")
        assertThat(params).doesNotContainKey("error_code")
    }

    @Test
    fun `express checkout confirmation context survives performer recreation`() = runScenario(
        apiConfiguration = DEFAULT_API_CONFIG.copy(publishableKey = "pk_test_restored_fallback"),
    ) {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)
        val initialPublishableKey = requireNotNull(stateHolder.state).paymentMethodMetadata.apiConfiguration.publishableKey
        performer.onExpressCheckoutElementConfirmationStarted(selection)
        val restoredPerformer = recreatePerformer()
        stateHolder.state = null
        startReporting(restoredPerformer)
        timeSource += 2.seconds

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        val events = events(count = 2)
        assertThat(events).hasSize(2)
        assertEvent(
            events.first(),
            ExpectedEvent(
                eventName = "checkout_elements.confirm.started",
                publishableKey = initialPublishableKey,
                confirmationParams = expressCheckoutConfirmationParams,
            ),
        )
        val terminal = events.last()
        assertEvent(
            terminal,
            ExpectedEvent(
                eventName = "checkout_elements.confirm.succeeded",
                publishableKey = "pk_test_restored_fallback",
                confirmationParams = expressCheckoutConfirmationParams,
            ),
        )
        assertThat(terminal).doesNotContainKey("duration")
    }

    @Test
    fun `express checkout uses its own Link session metadata`() = runScenario {
        stateHolder.state = stateHolder.state!!.copy(
            paymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedIn),
            expressCheckoutElementPaymentMethodMetadata = linkMetadata(LinkState.LoginState.NeedsVerification),
        )

        performer.onExpressCheckoutElementConfirmationStarted(PaymentSelection.Link(brand = LinkBrand.Link))

        val params = events(count = 1).single()
        assertEvent(
            params,
            ExpectedEvent(
                eventName = "checkout_elements.confirm.started",
                publishableKey = currentPublishableKey(),
                confirmationParams = expressCheckoutConfirmationParams + ("active_link_session" to false),
            ),
        )
    }

    @Test
    fun `confirmation response metadata wins over refreshed state`() = runScenario {
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        stateHolder.state = stateHolder.state!!.copy(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                id = "cs_refreshed_confirmation",
                currency = "eur",
            )
        )
        val confirmationResponse = CheckoutSessionResponseFactory.create(
            id = "cs_confirmed",
            currency = "cad",
        )
        val resultWithResponse = successResult.copy(
            metadata = MutableConfirmationMetadata().apply {
                set(CheckoutSessionResponseKey, confirmationResponse)
            }
        )
        startReporting(performer)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(resultWithResponse)

        val terminal = events(count = 2).last()
        assertEvent(
            terminal,
            ExpectedEvent(
                eventName = "checkout_elements.confirm.succeeded",
                publishableKey = currentPublishableKey(),
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertThat(terminal).containsEntry("checkout_session_id", "cs_confirmed")
        assertThat(terminal).containsEntry("currency", "cad")
    }

    @Test
    fun `initial config id survives a terminal response without config id`() = runScenario {
        val initialResponse = CheckoutSessionResponseFactory.create(
            id = "cs_initial_confirmation",
            configId = "cfg_initial_confirmation",
        )
        stateHolder.state = stateHolder.state!!.copy(checkoutSessionResponse = initialResponse)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        stateHolder.state = stateHolder.state!!.copy(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                id = "cs_refreshed_confirmation",
                configId = null,
            )
        )
        startReporting(performer)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        val terminal = events(count = 2).last()
        assertEvent(
            terminal,
            ExpectedEvent(
                eventName = "checkout_elements.confirm.succeeded",
                publishableKey = currentPublishableKey(),
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertThat(terminal).containsEntry("checkout_session_id", "cs_refreshed_confirmation")
        assertThat(terminal).containsEntry("checkout_config_id", "cfg_initial_confirmation")
    }

    @Test
    fun `confirmation response metadata wins over refreshed state and provides request id`() = runScenario {
        val initialResponse = CheckoutSessionResponseFactory.create(
            id = "cs_initial_confirmation",
            configId = "cfg_initial_confirmation",
        )
        stateHolder.state = stateHolder.state!!.copy(checkoutSessionResponse = initialResponse)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        stateHolder.state = stateHolder.state!!.copy(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                id = "cs_refreshed_confirmation",
                configId = "cfg_refreshed_confirmation",
                requestId = "req_refreshed_state",
            )
        )
        val confirmationResponse = CheckoutSessionResponseFactory.create(
            id = "cs_confirmed",
            configId = "cfg_confirmed",
            requestId = "req_confirmed",
        )
        val resultWithResponse = successResult.copy(
            metadata = MutableConfirmationMetadata().apply {
                set(CheckoutSessionResponseKey, confirmationResponse)
            }
        )
        startReporting(performer)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(resultWithResponse)

        val terminal = events(count = 2).last()
        assertEvent(
            terminal,
            ExpectedEvent(
                eventName = "checkout_elements.confirm.succeeded",
                publishableKey = currentPublishableKey(),
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertThat(terminal).containsEntry("checkout_session_id", "cs_confirmed")
        assertThat(terminal).containsEntry("checkout_config_id", "cfg_confirmed")
        assertThat(terminal).containsEntry("request_id", "req_confirmed")
        assertThat(terminal).doesNotContainValue("req_refreshed_state")
    }

    @Test
    fun `success without confirmation response omits refreshed request id`() = runScenario {
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        stateHolder.state = stateHolder.state!!.copy(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                requestId = "req_refreshed_state",
            )
        )
        startReporting(performer)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        val terminal = events(count = 2).last()
        assertEvent(
            terminal,
            ExpectedEvent(
                eventName = "checkout_elements.confirm.succeeded",
                publishableKey = currentPublishableKey(),
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertThat(terminal).doesNotContainKey("request_id")
    }

    private fun assertFailureReported(type: ConfirmationHandler.Result.Failed.ErrorType) = runScenario {
        startReporting(performer)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("private failure details"),
                message = "Unable to confirm".resolvableString,
                type = type,
            )
        )

        val events = events(count = 2)
        assertThat(events.map { it["event"] }).containsExactly(
            "checkout_elements.confirm.started",
            "checkout_elements.confirm.failed",
        ).inOrder()
        val publishableKey = currentPublishableKey()
        assertEvent(
            events.first(),
            ExpectedEvent(
                eventName = "checkout_elements.confirm.started",
                publishableKey = publishableKey,
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertEvent(
            events.last(),
            ExpectedEvent(
                eventName = "checkout_elements.confirm.failed",
                publishableKey = publishableKey,
                confirmationParams = paymentElementConfirmationParams,
            ),
        )
        assertThat(events.last()).containsEntry("error_type", "IllegalStateException")
        assertThat(events.last().toString()).doesNotContain("private failure details")
    }

    private fun linkMetadata(loginState: LinkState.LoginState): PaymentMethodMetadata =
        PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = LinkTestUtils.createLinkConfiguration(),
                loginState = loginState,
                signupMode = null,
            )
        )

    private fun assertEvent(
        params: Map<String, *>,
        expected: ExpectedEvent,
    ) {
        assertThat(params).containsEntry("event", expected.eventName)
        assertThat(params).containsEntry("publishable_key", expected.publishableKey)
        assertThat(params).containsEntry("product_usage", "checkout_elements")
        assertThat(params).doesNotContainKey("payment_method_orientation")
        expected.confirmationParams.forEach { (key, value) ->
            assertThat(params).containsEntry(key, value)
        }
    }

    private fun runScenario(
        apiConfiguration: ApiConfiguration.State = DEFAULT_API_CONFIG,
        workContextFactory: (TestCoroutineScheduler) -> TestDispatcher = { UnconfinedTestDispatcher(it) },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle).apply {
            state = CheckoutControllerStateFactory.create()
        }
        val linkAccountHolder = LinkAccountHolder(savedStateHandle)
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val timeSource = TestTimeSource()
        val apiConfigurationProvider = Provider { apiConfiguration }
        val scenario = Scenario(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            linkAccountHolder = linkAccountHolder,
            analyticsRequestExecutor = analyticsRequestExecutor,
            timeSource = timeSource,
            apiConfigurationProvider = apiConfigurationProvider,
            workContext = workContextFactory(testScheduler),
            backgroundScope = backgroundScope,
            testScheduler = testScheduler,
        )

        scenario.block()

        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        analyticsRequestExecutor.validate()
        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationHandler: FakeConfirmationHandler,
        private val savedStateHandle: SavedStateHandle,
        val stateHolder: CheckoutControllerStateHolder,
        private val linkAccountHolder: LinkAccountHolder,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val timeSource: TestTimeSource,
        val apiConfigurationProvider: Provider<ApiConfiguration.State>,
        private val workContext: TestDispatcher,
        private val backgroundScope: CoroutineScope,
        private val testScheduler: TestCoroutineScheduler,
    ) {
        val performer = createPerformer()

        fun recreatePerformer(): CheckoutAnalyticsPerformer = createPerformer()

        fun startReporting(performer: CheckoutAnalyticsPerformer) {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                performer.reportConfirmationResults()
            }
        }

        suspend fun events(count: Int): List<Map<String, *>> = List(count) {
            analyticsRequestExecutor.requestTurbine.awaitItem().params
        }

        fun currentPublishableKey(): String = requireNotNull(stateHolder.state)
            .paymentMethodMetadata.apiConfiguration.publishableKey

        private fun createPerformer(): CheckoutAnalyticsPerformer = CheckoutAnalyticsPerformer(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            linkAccountHolder = linkAccountHolder,
            context = ApplicationProvider.getApplicationContext<Context>(),
            analyticsRequestExecutor = analyticsRequestExecutor,
            apiConfigurationProvider = apiConfigurationProvider,
            timeSource = timeSource,
            workContext = workContext,
        )
    }

    private data class ExpectedEvent(
        val eventName: String,
        val publishableKey: String,
        val confirmationParams: Map<String, Any> = emptyMap(),
    )

    private companion object {
        val successResult = ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        val paymentElementConfirmationParams = mapOf(
            "selected_lpm" to "google_pay",
            "invocation_source" to "merchant",
        )
        val expressCheckoutConfirmationParams = mapOf(
            "selected_lpm" to "link",
            "link_context" to "wallet",
            "invocation_source" to "express_checkout_element",
        )
    }
}
