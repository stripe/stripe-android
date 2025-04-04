package com.stripe.android.paymentsheet

import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.Stripe
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventRule
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
@RunWith(TestParameterInjector::class)
internal class PaymentSheetAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 1.seconds, // Analytics requests happen async.
    )
    private val analyticEventRule = AnalyticEventRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(analyticEventRule)
    }

    private val composeTestRule = testRules.compose

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    private val verticalModeConfiguration = PaymentSheet.Configuration.Builder("Example, Inc.")
        .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
        .build()

    private val horizontalModeConfiguration = PaymentSheet.Configuration(
        merchantDisplayName = "Example, Inc.",
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    )

    @Before
    fun setup() {
        Stripe.advancedFraudSignalsEnabled = false
        GooglePayRepository.googlePayAvailabilityClientFactory = createFakeGooglePayAvailabilityClient()
    }

    @After
    fun teardown() {
        Stripe.advancedFraudSignalsEnabled = true
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testSuccessfulCardPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        analyticEventCallback = analyticEventRule,
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
                configuration = horizontalModeConfiguration,
            )
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

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
        analyticEventCallback = analyticEventRule,
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
                configuration = horizontalModeConfiguration,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

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

    @Test
    fun testSuccessfulCardPaymentInVerticalMode() = runPaymentSheetTest(
        networkRule = networkRule,
        analyticEventCallback = analyticEventRule,
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
        validateAnalyticsRequest(eventName = "mc_carousel_payment_method_tapped")
        validateAnalyticsRequest(eventName = "mc_form_shown")

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = verticalModeConfiguration,
            )
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        validateAnalyticsRequest(eventName = "mc_form_interacted")
        validateAnalyticsRequest(eventName = "mc_card_number_completed")

        page.clickOnLpm("card", forVerticalMode = true)
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedPaymentMethodType("card"))

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
    fun testSuccessfulCardPaymentInFlowControllerInVerticalMode() = runFlowControllerTest(
        networkRule = networkRule,
        analyticEventCallback = analyticEventRule,
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
                configuration = verticalModeConfiguration,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        validateAnalyticsRequest(eventName = "mc_carousel_payment_method_tapped")
        validateAnalyticsRequest(eventName = "mc_custom_paymentoption_newpm_select")
        validateAnalyticsRequest(eventName = "mc_form_interacted")
        validateAnalyticsRequest(eventName = "mc_card_number_completed")

        page.clickOnLpm("card", forVerticalMode = true)
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedPaymentMethodType("card"))

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
        networkRule.validateAnalyticsRequest(eventName, *requestMatchers)
    }

    private fun createFakeGooglePayAvailabilityClient(): GooglePayAvailabilityClient.Factory {
        return object : GooglePayAvailabilityClient.Factory {
            override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                return object : GooglePayAvailabilityClient {
                    override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
                        return true
                    }
                }
            }
        }
    }
}
