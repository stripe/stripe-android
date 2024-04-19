package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.IntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import com.stripe.android.testing.RetryRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(TestParameterInjector::class)
internal class PaymentSheetAnalyticsTest {
    private val retryRule = RetryRule(5)
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 1.seconds, // Analytics requests happen async.
    )
    private val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.emptyRuleChain()
        .around(networkRule)
        .around(composeTestRule)
        .around(retryRule)

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @TestParameter(valuesProvider = IntegrationTypeProvider::class)
    lateinit var integrationType: IntegrationType

    @Test
    fun testSuccessfulCardPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        validateAnalyticsRequest(eventName = "mc_complete_init_default")
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
        validateAnalyticsRequest(eventName = "mc_complete_sheet_newpm_show")
        validateAnalyticsRequest(eventName = "mc_form_shown")

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
            )
        }

        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        validateAnalyticsRequest(eventName = "mc_form_interacted")
        validateAnalyticsRequest(eventName = "mc_card_number_completed")

        page.fillOutCardDetails()

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        validateAnalyticsRequest(eventName = "mc_confirm_button_tapped")
        validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.started",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        validateAnalyticsRequest(eventName = "stripe_android.confirm_returnurl_null")
        validateAnalyticsRequest(eventName = "stripe_android.payment_intent_confirmation")
        validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.finished",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        validateAnalyticsRequest(eventName = "mc_complete_payment_newpm_success", hasQueryParam("duration"))

        page.clickPrimaryButton()
    }

    @Test
    fun testSuccessfulCardPaymentInFlowController() = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        validateAnalyticsRequest(eventName = "mc_custom_init_default")
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
        validateAnalyticsRequest(eventName = "mc_custom_sheet_newpm_show")
        validateAnalyticsRequest(eventName = "mc_form_shown")

        testContext.configureFlowController {
            configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        validateAnalyticsRequest(eventName = "mc_custom_paymentoption_newpm_select")
        validateAnalyticsRequest(eventName = "mc_form_interacted")
        validateAnalyticsRequest(eventName = "mc_card_number_completed")

        page.fillOutCardDetails()

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        validateAnalyticsRequest(eventName = "mc_confirm_button_tapped")
        validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.started",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        validateAnalyticsRequest(eventName = "stripe_android.confirm_returnurl_null")
        validateAnalyticsRequest(eventName = "stripe_android.payment_intent_confirmation")
        validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.finished",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        validateAnalyticsRequest(eventName = "mc_custom_payment_newpm_success", hasQueryParam("duration"))

        page.clickPrimaryButton()
    }

    private fun validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.enqueue(
            host("q.stripe.com"),
            method("GET"),
            query("event", eventName),
            *requestMatchers,
        ) { response ->
            response.status = "HTTP/1.1 200 OK"
        }
    }
}
