package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.injection.DaggerCheckoutElementsControllerComponent
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.elements.PaymentElement
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutConfigureAnalyticsTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule(hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST))
    private val cleanup = CleanupTestRule(CheckoutElementsController::destroy)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(cleanup)
        .around(AdvancedFraudSignalsTestRule())
        .around(networkRule)
        .around(PaymentConfigurationTestRule(application))
        .around(CoroutineTestRule(StandardTestDispatcher()))

    @Test
    fun `success reports attempted configuration and published session`() = runScenario {
        expectInit("succeeded")
        networkRule.checkoutInit { response -> successResponse(response) }
        val configuration = CheckoutElementsController.Configuration()
            .merchantDisplayName("Example")
            .defaults(CheckoutElementsController.Configuration.Defaults().email("customer@example.com"))

        val result = controller.configure(CLIENT_SECRET, configuration)

        assertThat(result).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        val started = startedEvents.awaitItem()
        assertThat(started).containsEntry("merchant_display_name", "true")
        assertThat(started).containsEntry("defaults[email]", "true")
        assertThat(started).doesNotContainKey("checkout_config_id")
        val finished = finishedEvents.awaitItem()
        assertThat(finished.params).containsEntry("merchant_display_name", "true")
        assertThat(finished.params).containsEntry("checkout_config_id", "checkout_config_123")
        assertThat(finished.publishedSessionId).isEqualTo(SESSION_ID)
    }

    @Test
    fun `request failure reports initialization failure`() = runScenario {
        expectInit("failed", analyticsPayloadField("error_code", "checkout_session_invalid"))
        networkRule.checkoutInit { response ->
            response.setResponseCode(400)
            response.addHeader("Request-Id", "req_init")
            response.setBody(
                """{"error":{"type":"invalid_request_error","code":"checkout_session_invalid","message":"Invalid"}}"""
            )
        }

        val result = controller.configure(CLIENT_SECRET)

        assertThat(result).isInstanceOf(CheckoutElementsController.OperationResult.Failed::class.java)
        startedEvents.awaitItem()
        assertThat(finishedEvents.awaitItem().params).containsEntry("request_id", "req_init")
    }

    @Test
    fun `state loading failure after a successful init request reports initialization failure`() = runScenario {
        expectInit("failed")
        // Robolectric has no Google Play services for the loader's two readiness checks.
        repeat(2) {
            networkRule.enqueue(
                host("q.stripe.com"),
                analyticsPayloadField("event", "elements.google_pay_repository.is_ready_request_api_call_failure"),
                applyDefaultAuthorization = false,
            ) { response -> response.setBody("{}") }
        }
        networkRule.checkoutInit { response ->
            successResponse(response) {
                it.put("ordered_payment_method_types_and_wallets", JSONArray().put("unsupported"))
            }
        }

        val result = controller.configure(
            CLIENT_SECRET,
            CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
        )

        assertThat(result).isInstanceOf(CheckoutElementsController.OperationResult.Failed::class.java)
        assertThat((result as CheckoutElementsController.OperationResult.Failed).error)
            .isInstanceOf(PaymentSheetLoadingException.NoPaymentMethodTypesAvailable::class.java)
        startedEvents.awaitItem()
        val finished = finishedEvents.awaitItem()
        assertThat(finished.params).containsEntry("checkout_config_id", "checkout_config_123")
        assertThat(finished.publishedSessionId).isNull()
    }

    @Test
    fun `tax failure after a successful init request reports initialization failure`() = runScenario {
        expectInit("failed", analyticsPayloadField("error_code", "tax_failed"))
        expectEvent("update_shipping_address.started")
        expectEvent("update_shipping_address.failed")
        networkRule.checkoutInit { response -> successResponse(response, ::shippingTax) }
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"type":"invalid_request_error","code":"tax_failed"}}""")
        }

        val result = controller.configure(CLIENT_SECRET, shippingConfiguration())

        assertThat(result).isInstanceOf(CheckoutElementsController.OperationResult.Failed::class.java)
        startedEvents.awaitItem()
        val finished = finishedEvents.awaitItem()
        assertThat(finished.params).containsEntry("checkout_config_id", "checkout_config_123")
        assertThat(finished.publishedSessionId).isNull()
    }

    @Test
    fun `failed reconfigure reports its own response and configuration instead of the published state`() = runScenario {
        expectInit("succeeded")
        networkRule.checkoutInit { response -> successResponse(response) }
        controller.configure(CLIENT_SECRET)
        startedEvents.awaitItem()
        finishedEvents.awaitItem()
        val publishedSession = controller.session.value

        expectInit("failed", analyticsPayloadField("merchant_display_name", "true"))
        expectEvent("update_shipping_address.started")
        expectEvent("update_shipping_address.failed")
        networkRule.checkoutInit { response ->
            successResponse(response) {
                shippingTax(it)
                it.put("checkout_session_config_id", "new_attempt_config")
            }
        }
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"type":"invalid_request_error","message":"Invalid tax region"}}""")
        }

        val result = controller.configure(CLIENT_SECRET, shippingConfiguration().merchantDisplayName("New"))

        assertThat(result).isInstanceOf(CheckoutElementsController.OperationResult.Failed::class.java)
        assertThat(startedEvents.awaitItem()).doesNotContainKey("checkout_config_id")
        assertThat(finishedEvents.awaitItem().params).containsEntry("checkout_config_id", "new_attempt_config")
        assertThat(controller.session.value).isEqualTo(publishedSession)
    }

    @Test
    fun `success waits for tax and state publication and includes their latency`() = runScenario {
        expectInit("succeeded")
        expectEvent("update_shipping_address.started")
        expectEvent("update_shipping_address.succeeded")
        networkRule.checkoutInit { response -> successResponse(response, ::shippingTax) }
        val taxRequested = Turbine<Unit>()
        val releaseTax = CountDownLatch(1)
        networkRule.checkoutUpdate { response ->
            taxRequested.add(Unit)
            check(releaseTax.await(10, TimeUnit.SECONDS))
            response.setBodyDelay(250, TimeUnit.MILLISECONDS)
            successResponse(response) {
                shippingTax(it)
                it.put("checkout_session_config_id", "config_after_tax")
            }
        }

        val configure = async { controller.configure(CLIENT_SECRET, shippingConfiguration()) }
        try {
            startedEvents.awaitItem()
            taxRequested.awaitItem()
            assertThat(configure.isCompleted).isFalse()
            assertThat(controller.session.value).isNull()
            finishedEvents.expectNoEvents()
            releaseTax.countDown()

            assertThat(configure.await()).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
            val finished = finishedEvents.awaitItem()
            assertThat(finished.params.getValue("duration").toDouble()).isAtLeast(0.25)
            assertThat(finished.params).containsEntry("checkout_config_id", "config_after_tax")
            assertThat(finished.publishedSessionId).isEqualTo(SESSION_ID)
        } finally {
            releaseTax.countDown()
        }
        taxRequested.ensureAllEventsConsumed()
    }

    @Test
    fun `invalid secret reports a failed attempt without exposing the secret`() = runScenario {
        expectInit("failed", hasQueryParam("error_type"))

        val result = controller.configure("invalid_secret_value")

        assertThat(result).isInstanceOf(CheckoutElementsController.OperationResult.Failed::class.java)
        assertThat(startedEvents.awaitItem().values).doesNotContain("invalid_secret_value")
        assertThat(finishedEvents.awaitItem().params.values).doesNotContain("invalid_secret_value")
    }

    @Test
    fun `open sheet rejection reports a failed attempt`() = runScenario {
        SheetStateHolder(savedState.handle).sheetIsOpen = true
        expectInit("failed", analyticsPayloadField("error_type", "java.lang.IllegalStateException"))

        val result = controller.configure(CLIENT_SECRET)

        assertThat(result).isInstanceOf(CheckoutElementsController.OperationResult.Failed::class.java)
        startedEvents.awaitItem()
        finishedEvents.awaitItem()
    }

    @Test
    fun `canceling a queued configure reports failure and preserves cancellation`() = runScenario {
        expectInit("succeeded")
        val initRequested = Turbine<Unit>()
        val releaseInit = CountDownLatch(1)
        networkRule.checkoutInit { response ->
            initRequested.add(Unit)
            check(releaseInit.await(10, TimeUnit.SECONDS))
            successResponse(response)
        }
        val first = async { controller.configure(CLIENT_SECRET) }
        try {
            startedEvents.awaitItem()
            initRequested.awaitItem()
            expectInit("failed", analyticsPayloadField("merchant_display_name", "true"))
            val queued = async {
                controller.configure(
                    CLIENT_SECRET,
                    CheckoutElementsController.Configuration().merchantDisplayName("New"),
                )
            }
            assertThat(startedEvents.awaitItem()).containsEntry("merchant_display_name", "true")
            queued.cancelAndJoin()
            val thrown = runCatching { queued.await() }.exceptionOrNull()
            assertThat(thrown).isInstanceOf(CancellationException::class.java)
            val canceledEvent = finishedEvents.awaitItem()
            assertThat(canceledEvent.params.getValue("event")).endsWith("init.failed")
            assertThat(canceledEvent.params).doesNotContainKey("checkout_config_id")

            releaseInit.countDown()
            assertThat(first.await()).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
            val firstEvent = finishedEvents.awaitItem()
            assertThat(firstEvent.params).containsEntry("merchant_display_name", "false")
            assertThat(firstEvent.params.getValue("duration").toDouble())
                .isGreaterThan(canceledEvent.params.getValue("duration").toDouble())
        } finally {
            releaseInit.countDown()
        }
        initRequested.ensureAllEventsConsumed()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val savedState = CheckoutElementsControllerSavedState(SavedStateHandle(), "configure_analytics")
        val controller = cleanup.track(
            DaggerCheckoutElementsControllerComponent.factory().create(
                application = application,
                paymentElementCallbackIdentifier = "configure_analytics",
                resultCallback = CheckoutElementsController.ResultCallback {},
                rowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
                checkoutElementsControllerSavedState = savedState,
            ).checkoutElementsController
        )
        val scenario = Scenario(controller, savedState, this)
        scenario.block()
        scenario.startedEvents.ensureAllEventsConsumed()
        scenario.finishedEvents.ensureAllEventsConsumed()
    }

    private inner class Scenario(
        val controller: CheckoutElementsController,
        val savedState: CheckoutElementsControllerSavedState,
        testScope: TestScope,
    ) : CoroutineScope by testScope {
        val startedEvents = Turbine<Map<String, String>>()
        val finishedEvents = Turbine<FinishedEvent>()

        fun expectInit(outcome: String, vararg matchers: RequestMatcher) {
            networkRule.enqueue(
                host("q.stripe.com"),
                RequestMatcher { CLIENT_SECRET !in it.queryParams.values },
                analyticsPayloadField("event", "checkout_elements.init.started"),
                not(hasQueryParam("duration")),
                applyDefaultAuthorization = false,
            ) { request, response ->
                startedEvents.add(request.queryParams)
                response.setBody("{}")
            }
            networkRule.enqueue(
                host("q.stripe.com"),
                RequestMatcher { CLIENT_SECRET !in it.queryParams.values },
                analyticsPayloadField("event", "checkout_elements.init.$outcome"),
                hasQueryParam("duration"),
                *matchers,
                applyDefaultAuthorization = false,
            ) { request, response ->
                finishedEvents.add(FinishedEvent(request.queryParams, controller.session.value?.id))
                response.setBody("{}")
            }
        }
    }

    private data class FinishedEvent(val params: Map<String, String>, val publishedSessionId: String?)

    private fun expectEvent(operationAndOutcome: String) {
        networkRule.enqueue(
            host("q.stripe.com"),
            analyticsPayloadField("event", "checkout_elements.$operationAndOutcome"),
            applyDefaultAuthorization = false,
        ) { response -> response.setBody("{}") }
    }

    private fun successResponse(response: MockResponse, modify: (JSONObject) -> Unit = {}) {
        response.testBodyFromFile("checkout-session-init.json") {
            it.put("customer_email", "checkout@example.com")
            it.remove("link_settings")
            modify(it)
        }
    }

    private fun shippingTax(json: JSONObject) {
        json.put(
            "automatic_tax",
            JSONObject().put("enabled", true).put("status", "requires_address").put("address_source", "shipping"),
        )
    }

    private fun shippingConfiguration() = CheckoutElementsController.Configuration().defaults(
        CheckoutElementsController.Configuration.Defaults().shippingDetails(
            CheckoutElementsController.Configuration.Defaults.ContactDetails()
                .address(CheckoutElementsController.Address("US")),
        ),
    )

    private companion object {
        const val SESSION_ID = "cs_test_abc123"
        const val CLIENT_SECRET = "${SESSION_ID}_secret_example"
    }
}
