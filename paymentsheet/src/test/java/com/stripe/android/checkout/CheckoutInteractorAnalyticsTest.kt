package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.Stripe
import com.stripe.android.checkout.injection.DaggerCheckoutElementsControllerComponent
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutFetch
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.elements.PaymentElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutInteractorAnalyticsTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )
    private val destroyControllerRule = CleanupTestRule(CheckoutElementsController::destroy)

    // Fraud detection requests fail under NetworkRule and report their own analytics, which would
    // interleave with the checkout events these tests enqueue. PaymentConfigurationTestRule refreshes
    // fraud detection data, so this must wrap it.
    private val disableFraudDetectionRule = object : ExternalResource() {
        override fun before() {
            Stripe.advancedFraudSignalsEnabled = false
        }

        override fun after() {
            Stripe.advancedFraudSignalsEnabled = true
        }
    }

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(disableFraudDetectionRule)
        .around(destroyControllerRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CoroutineTestRule(StandardTestDispatcher()))

    @After
    fun clearCallbackReferences() {
        PaymentElementCallbackReferences.clear()
    }

    @Test
    fun `configure reports init with the committed session params`() = runScenario {
        expectEvent(
            "init.started",
            doesNotContainValue(CLIENT_SECRET),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            analyticsPayloadField("merchant_display_name", "false"),
            analyticsPayloadField("defaults[email]", "false"),
            not(hasQueryParam("ordered_lpms")),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "init.succeeded",
            doesNotContainValue(CLIENT_SECRET),
            *INIT_RESPONSE_SESSION_PARAMS.asPayloadMatchers(),
            analyticsPayloadField("merchant_display_name", "false"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        assertSucceeded(controller.configure(CLIENT_SECRET, CheckoutElementsController.Configuration()))
    }

    @Test
    fun `configure reports init failure with the error`() = runScenario {
        expectEvent(
            "init.started",
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "init.failed",
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            analyticsPayloadField("error_type", "invalid_request_error"),
            analyticsPayloadField("error_code", "checkout_session_invalid"),
            analyticsPayloadField("request_id", "req_123"),
            not(hasQueryParam("ordered_lpms")),
            hasQueryParam("duration"),
        )
        networkRule.checkoutInit { response ->
            response.setResponseCode(400)
            response.addHeader("Request-Id", "req_123")
            response.setBody(INVALID_REQUEST_ERROR_BODY)
        }

        assertFailed(controller.configure(CLIENT_SECRET, CheckoutElementsController.Configuration()))
    }

    @Test
    fun `configure reports init failure for a malformed client secret`() = runScenario {
        expectEvent(
            "init.started",
            not(hasQueryParam("checkout_session_id")),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "init.failed",
            not(hasQueryParam("checkout_session_id")),
            analyticsPayloadField("error_type", "invalidRequestError"),
            hasQueryParam("duration"),
        )

        assertFailed(controller.configure("not_a_client_secret", CheckoutElementsController.Configuration()))
    }

    @Test
    fun `configure reports nested tax update inside init`() = runScenario {
        expectEvent("init.started")
        expectEvent(
            "update_billing_address.started",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )
        expectEvent("update_billing_address.succeeded", hasQueryParam("duration"))
        expectEvent(
            "init.succeeded",
            analyticsPayloadField("automatic_tax_enabled", "true"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json", ::automaticBillingTax)
        }
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json", ::automaticBillingTax)
        }

        assertSucceeded(
            controller.configure(
                CLIENT_SECRET,
                CheckoutElementsController.Configuration().defaults(
                    CheckoutElementsController.Configuration.Defaults().billingDetails(
                        CheckoutElementsController.Configuration.Defaults.ContactDetails().address(
                            CheckoutElementsController.Address(country = "US")
                        )
                    )
                ),
            )
        )
    }

    @Test
    fun `applyPromotionCode reports the committed session params on success`() = runConfiguredScenario {
        expectEvent(
            "apply_promotion_code.started",
            *INIT_RESPONSE_SESSION_PARAMS.asPayloadMatchers(),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "apply_promotion_code.succeeded",
            analyticsPayloadField("checkout_config_id", "checkout_config_updated"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.put("checkout_session_config_id", "checkout_config_updated")
            }
        }

        assertSucceeded(controller.applyPromotionCode("SAVE10"))
    }

    @Test
    fun `applyPromotionCode reports failure with the error`() = runConfiguredScenario {
        expectEvent("apply_promotion_code.started")
        expectEvent(
            "apply_promotion_code.failed",
            *INIT_RESPONSE_SESSION_PARAMS.asPayloadMatchers(),
            analyticsPayloadField("error_type", "invalid_request_error"),
            analyticsPayloadField("error_code", "checkout_session_invalid"),
            analyticsPayloadField("request_id", "req_123"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.addHeader("Request-Id", "req_123")
            response.setBody(INVALID_REQUEST_ERROR_BODY)
        }

        assertFailed(controller.applyPromotionCode("SAVE10"))
    }

    @Test
    fun `applyPromotionCode reports failure before configure`() = runScenario {
        expectEvent("apply_promotion_code.started", not(hasQueryParam("checkout_session_id")))
        expectEvent(
            "apply_promotion_code.failed",
            analyticsPayloadField("error_type", "java.lang.IllegalStateException"),
            hasQueryParam("duration"),
        )

        assertFailed(controller.applyPromotionCode("SAVE10"))
    }

    @Test
    fun `removePromotionCode reports started and succeeded`() = runConfiguredScenario {
        expectEvent("remove_promotion_code.started", not(hasQueryParam("duration")))
        expectEvent("remove_promotion_code.succeeded", hasQueryParam("duration"))
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        assertSucceeded(controller.removePromotionCode())
    }

    @Test
    fun `updateCurrency reports the requested currency`() = runConfiguredScenario {
        expectEvent("update_currency.started", analyticsPayloadField("requested_currency", "eur"))
        expectEvent(
            "update_currency.succeeded",
            analyticsPayloadField("requested_currency", "eur"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        assertSucceeded(interactor.updateCurrency("eur"))
    }

    @Test
    fun `updateShippingAddress without a tax update reports started and succeeded`() = runConfiguredScenario {
        expectEvent(
            "update_shipping_address.started",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "false"),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "update_shipping_address.succeeded",
            analyticsPayloadField("did_request_tax_update", "false"),
            hasQueryParam("duration"),
        )

        assertSucceeded(
            controller.updateShippingAddress(
                name = "Jenny",
                address = CheckoutElementsController.Address(country = "US"),
            )
        )
    }

    @Test
    fun `updateShippingAddress reports failure for an unsupported country`() = runConfiguredScenario(
        initModifier = { json ->
            json.put(
                "shipping_address_collection",
                JSONObject().put("allowed_countries", JSONArray().put("US")),
            )
        },
    ) {
        expectEvent("update_shipping_address.started")
        expectEvent("update_shipping_address.failed", hasQueryParam("duration"))

        assertFailed(
            controller.updateShippingAddress(
                name = "Jenny",
                address = CheckoutElementsController.Address(country = "FR"),
            )
        )
    }

    @Test
    fun `runServerUpdate reports the sub-durations on success`() = runConfiguredScenario {
        expectEvent(
            "run_server_update.started",
            not(hasQueryParam("merchant_update_duration")),
            not(hasQueryParam("refresh_duration")),
        )
        expectEvent(
            "run_server_update.succeeded",
            hasQueryParam("merchant_update_duration"),
            hasQueryParam("refresh_duration"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutFetch { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        assertSucceeded(controller.runServerUpdate { Result.success(Unit) })
    }

    @Test
    fun `runServerUpdate reports failure from the merchant update`() = runConfiguredScenario {
        expectEvent("run_server_update.started")
        expectEvent(
            "run_server_update.failed",
            hasQueryParam("merchant_update_duration"),
            not(hasQueryParam("refresh_duration")),
            hasQueryParam("duration"),
        )

        assertFailed(controller.runServerUpdate { Result.failure(IllegalStateException("boom")) })
    }

    private fun runConfiguredScenario(
        initModifier: (JSONObject) -> Unit = {},
        block: suspend Scenario.() -> Unit,
    ) = runScenario {
        expectEvent("init.started")
        expectEvent("init.succeeded")
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json", initModifier)
        }
        assertSucceeded(controller.configure(CLIENT_SECRET, CheckoutElementsController.Configuration()))
        networkRule.validate()
        block()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val controllerSavedState = CheckoutElementsControllerSavedState(
            parentHandle = SavedStateHandle(),
            integrationName = INTEGRATION_NAME,
        )
        // Creating LiveData for the state keys makes Android reject off-main state publication.
        controllerSavedState.handle.getLiveData<CheckoutElementsControllerState>(
            CheckoutElementsControllerStateHolder.STATE_KEY
        )
        controllerSavedState.handle.getLiveData<CustomerState>(CustomerStateHolder.SAVED_CUSTOMER)
        val component = DaggerCheckoutElementsControllerComponent.factory().create(
            application = applicationContext,
            paymentElementCallbackIdentifier = INTEGRATION_NAME,
            resultCallback = CheckoutElementsController.ResultCallback {},
            rowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
            checkoutElementsControllerSavedState = controllerSavedState,
        )
        Scenario(
            controller = destroyControllerRule.track(component.checkoutElementsController),
            interactor = component.checkoutInteractor,
        ).block()
    }

    private class Scenario(
        val controller: CheckoutElementsController,
        val interactor: CheckoutInteractor,
    )

    private fun assertSucceeded(result: CheckoutElementsController.OperationResult) {
        assertThat(result).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
    }

    private fun assertFailed(result: CheckoutElementsController.OperationResult) {
        assertThat(result).isInstanceOf(CheckoutElementsController.OperationResult.Failed::class.java)
    }

    private fun expectEvent(operationAndOutcome: String, vararg matchers: RequestMatcher) {
        networkRule.enqueue(
            host("q.stripe.com"),
            method("GET"),
            analyticsPayloadField("event", "checkout_elements.$operationAndOutcome"),
            analyticsPayloadField("product_usage", "checkout_elements"),
            *matchers,
            applyDefaultAuthorization = false,
        ) { response ->
            response.status = "HTTP/1.1 200 OK"
        }
    }

    private fun Map<String, Any>.asPayloadMatchers(): Array<RequestMatcher> =
        map { (key, value) -> analyticsPayloadField(key, value.toString()) }.toTypedArray()

    private fun doesNotContainValue(value: String) = RequestMatcher { request ->
        value !in request.queryParams.values
    }

    private fun automaticBillingTax(json: JSONObject) {
        json.put(
            "automatic_tax",
            JSONObject()
                .put("enabled", true)
                .put("status", "requires_address")
                .put("address_source", "billing"),
        )
    }

    private companion object {
        const val CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_test"
        const val INTEGRATION_NAME = "stripe_checkout"

        const val INVALID_REQUEST_ERROR_BODY = """
            {
                "error": {
                    "type": "invalid_request_error",
                    "code": "checkout_session_invalid",
                    "message": "Invalid session"
                }
            }
        """

        // Values come from checkout-session-init.json.
        val INIT_RESPONSE_SESSION_PARAMS: Map<String, Any> = mapOf(
            "checkout_session_id" to DEFAULT_CHECKOUT_SESSION_ID,
            "checkout_config_id" to "checkout_config_123",
            "currency" to "usd",
            "session_status" to "open",
            "payment_status" to "unpaid",
            "elements_session_config_id" to "cs_config_123",
            "adaptive_pricing_active" to false,
            "automatic_tax_enabled" to false,
            "ordered_lpms" to "card,link,apple_pay,google_pay,cashapp",
        )
    }
}
