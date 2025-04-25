package com.stripe.android.paymentsheet

import android.net.Uri
import com.google.common.truth.Truth.assertThat
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
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventRule
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentsheet.utils.AdvancedFraudSignalsTestRule
import com.stripe.android.paymentsheet.utils.FlowControllerTestRunnerContext
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupPaymentMethodDetachResponse
import com.stripe.paymentelementnetwork.setupV1PaymentMethodsResponse
import com.stripe.paymentelementtestpages.EditPage
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
            .around(AdvancedFraudSignalsTestRule())
            .around(GooglePayRepositoryTestRule())
    }

    private val composeTestRule = testRules.compose

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)
    private val editPage = EditPage(testRules.compose)

    private val card1 = CardPaymentMethodDetails("pm_12345", "4242")
    private val card2 = CardPaymentMethodDetails("pm_67890", "5544")

    private val verticalModeConfiguration = PaymentSheet.Configuration.Builder("Example, Inc.")
        .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
        .build()

    private val horizontalModeConfiguration = PaymentSheet.Configuration(
        merchantDisplayName = "Example, Inc.",
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    )

    @Test
    fun testSuccessfulCardPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        builder = {
            analyticEventCallback(analyticEventRule)
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

        testContext.validateAnalyticsRequest(
            eventName = "mc_complete_init_default",
            query(Uri.encode("mpe_config[analytic_callback_set]"), "true"),
        )
        testContext.validateAnalyticsRequest(eventName = "mc_load_started")
        testContext.validateAnalyticsRequest(eventName = "mc_load_succeeded")
        testContext.validateAnalyticsRequest(eventName = "mc_complete_sheet_newpm_show")
        testContext.validateAnalyticsRequest(eventName = "mc_form_shown")

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = horizontalModeConfiguration,
            )
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.DisplayedPaymentMethodForm("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        testContext.validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        testContext.validateAnalyticsRequest(eventName = "mc_form_interacted")
        testContext.validateAnalyticsRequest(eventName = "mc_card_number_completed")

        testContext.validateAnalyticsRequest(eventName = "mc_form_completed")
        page.fillOutCardDetails()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.StartedInteractionWithPaymentMethodForm("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.CompletedPaymentMethodForm("card"))

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        testContext.validateAnalyticsRequest(eventName = "mc_confirm_button_tapped")
        testContext.validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.started",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        testContext.validateAnalyticsRequest(eventName = "stripe_android.confirm_returnurl_null")
        testContext.validateAnalyticsRequest(eventName = "stripe_android.payment_intent_confirmation")
        testContext.validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.finished",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        testContext.validateAnalyticsRequest(
            eventName = "mc_complete_payment_newpm_success",
            hasQueryParam("duration")
        )

        page.clickPrimaryButton()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.TappedConfirmButton("card"))
    }

    @Test
    fun testSuccessfulCardPaymentInFlowController() = runFlowControllerTest(
        networkRule = networkRule,
        builder = {
            analyticEventCallback(analyticEventRule)
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

        testContext.validateAnalyticsRequest(
            eventName = "mc_custom_init_default",
            query(Uri.encode("mpe_config[analytic_callback_set]"), "true"),
        )
        testContext.validateAnalyticsRequest(eventName = "mc_load_started")
        testContext.validateAnalyticsRequest(eventName = "mc_load_succeeded")
        testContext.validateAnalyticsRequest(eventName = "mc_custom_sheet_newpm_show")
        testContext.validateAnalyticsRequest(eventName = "mc_form_shown")

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

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.DisplayedPaymentMethodForm("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        testContext.validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        testContext.validateAnalyticsRequest(eventName = "mc_custom_paymentoption_newpm_select")
        testContext.validateAnalyticsRequest(eventName = "mc_form_interacted")
        testContext.validateAnalyticsRequest(eventName = "mc_card_number_completed")

        testContext.validateAnalyticsRequest(eventName = "mc_form_completed")
        page.fillOutCardDetails()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.StartedInteractionWithPaymentMethodForm("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.CompletedPaymentMethodForm("card"))

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        testContext.validateAnalyticsRequest(eventName = "mc_confirm_button_tapped")
        testContext.validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.started",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        testContext.validateAnalyticsRequest(eventName = "stripe_android.confirm_returnurl_null")
        testContext.validateAnalyticsRequest(eventName = "stripe_android.payment_intent_confirmation")
        testContext.validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.finished",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        testContext.validateAnalyticsRequest(
            eventName = "mc_custom_payment_newpm_success",
            hasQueryParam("duration")
        )

        page.clickPrimaryButton()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.TappedConfirmButton("card"))
    }

    @Test
    fun testSuccessfulCardPaymentInVerticalMode() = runPaymentSheetTest(
        networkRule = networkRule,
        builder = {
            analyticEventCallback(analyticEventRule)
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

        testContext.validateAnalyticsRequest(eventName = "mc_complete_init_default")
        testContext.validateAnalyticsRequest(eventName = "mc_load_started")
        testContext.validateAnalyticsRequest(eventName = "mc_load_succeeded")
        testContext.validateAnalyticsRequest(eventName = "mc_complete_sheet_newpm_show")
        testContext.validateAnalyticsRequest(eventName = "mc_carousel_payment_method_tapped")
        testContext.validateAnalyticsRequest(eventName = "mc_form_shown")

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = verticalModeConfiguration,
            )
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        testContext.validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        testContext.validateAnalyticsRequest(eventName = "mc_form_interacted")
        testContext.validateAnalyticsRequest(eventName = "mc_card_number_completed")

        page.clickOnLpm("card", forVerticalMode = true)
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedPaymentMethodType("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.DisplayedPaymentMethodForm("card"))

        testContext.validateAnalyticsRequest(eventName = "mc_form_completed")
        page.fillOutCardDetails()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.StartedInteractionWithPaymentMethodForm("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.CompletedPaymentMethodForm("card"))

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        testContext.validateAnalyticsRequest(eventName = "mc_confirm_button_tapped")
        testContext.validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.started",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        testContext.validateAnalyticsRequest(eventName = "stripe_android.confirm_returnurl_null")
        testContext.validateAnalyticsRequest(eventName = "stripe_android.payment_intent_confirmation")
        testContext.validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.finished",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        testContext.validateAnalyticsRequest(
            eventName = "mc_complete_payment_newpm_success",
            hasQueryParam("duration")
        )

        page.clickPrimaryButton()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.TappedConfirmButton("card"))
    }

    @Test
    fun testSuccessfulCardPaymentInFlowControllerInVerticalMode() = runFlowControllerTest(
        networkRule = networkRule,
        builder = {
            analyticEventCallback(analyticEventRule)
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

        testContext.validateAnalyticsRequest(eventName = "mc_custom_init_default")
        testContext.validateAnalyticsRequest(eventName = "mc_load_started")
        testContext.validateAnalyticsRequest(eventName = "mc_load_succeeded")
        testContext.validateAnalyticsRequest(eventName = "mc_custom_sheet_newpm_show")
        testContext.validateAnalyticsRequest(eventName = "mc_form_shown")

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

        testContext.validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        testContext.validateAnalyticsRequest(eventName = "mc_carousel_payment_method_tapped")
        testContext.validateAnalyticsRequest(eventName = "mc_custom_paymentoption_newpm_select")
        testContext.validateAnalyticsRequest(eventName = "mc_form_interacted")
        testContext.validateAnalyticsRequest(eventName = "mc_card_number_completed")

        page.clickOnLpm("card", forVerticalMode = true)
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedPaymentMethodType("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.DisplayedPaymentMethodForm("card"))

        testContext.validateAnalyticsRequest(eventName = "mc_form_completed")
        page.fillOutCardDetails()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.StartedInteractionWithPaymentMethodForm("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.CompletedPaymentMethodForm("card"))

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        testContext.validateAnalyticsRequest(eventName = "mc_confirm_button_tapped")
        testContext.validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.started",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        testContext.validateAnalyticsRequest(eventName = "stripe_android.confirm_returnurl_null")
        testContext.validateAnalyticsRequest(eventName = "stripe_android.payment_intent_confirmation")
        testContext.validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.finished",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        testContext.validateAnalyticsRequest(
            eventName = "mc_custom_payment_newpm_success",
            hasQueryParam("duration")
        )

        page.clickPrimaryButton()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.TappedConfirmButton("card"))
    }

    @Test
    fun testSavedPaymentMethod() = runPaymentSheetTest(
        networkRule = networkRule,
        builder = {
            analyticEventCallback(analyticEventRule)
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_cvc_recollection.json")
        }
        networkRule.setupV1PaymentMethodsResponse(card1, card2)
        testContext.validateAnalyticsRequest(eventName = "mc_complete_init_customer")
        testContext.validateAnalyticsRequest(eventName = "mc_load_started")
        testContext.validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        testContext.validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")
        testContext.validateAnalyticsRequest(eventName = "mc_load_succeeded")
        testContext.validateAnalyticsRequest(eventName = "mc_complete_sheet_savedpm_show")

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = horizontalModeConfiguration.copy(
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123",
                    ),
                )
            )
        }
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        testContext.validateAnalyticsRequest(eventName = "mc_open_edit_screen")
        page.clickEditButton()
        page.clickSavedCardEditBadge(card1.last4)
        editPage.waitUntilVisible()

        networkRule.setupPaymentMethodDetachResponse(card1.id)
        testContext.validateAnalyticsRequest(eventName = "stripe_android.detach_payment_method")
        testContext.validateAnalyticsRequest(eventName = "mc_cancel_edit_screen")

        testContext.validateAnalyticsRequest(eventName = "mc_complete_paymentoption_removed")
        editPage.clickRemove()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.RemovedSavedPaymentMethod("card"))

        testContext.validateAnalyticsRequest(eventName = "mc_complete_paymentoption_savedpm_select")
        page.clickDoneButton()
        page.clickSavedCard(card2.last4)
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedSavedPaymentMethod("card"))
        testContext.markTestSucceeded()
    }

    @Test
    fun testSavedPaymentMethodInFlowController() = runFlowControllerTest(
        networkRule = networkRule,
        builder = {
            analyticEventCallback(analyticEventRule)
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_cvc_recollection.json")
        }

        networkRule.setupV1PaymentMethodsResponse(card1, card2)
        testContext.validateAnalyticsRequest(eventName = "mc_custom_init_customer")
        testContext.validateAnalyticsRequest(eventName = "mc_load_started")
        testContext.validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        testContext.validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")
        testContext.validateAnalyticsRequest(eventName = "mc_load_succeeded")
        testContext.validateAnalyticsRequest(eventName = "mc_custom_sheet_savedpm_show")

        testContext.configureFlowController {
            configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = horizontalModeConfiguration.copy(
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123",
                    ),
                ),
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        testContext.validateAnalyticsRequest(eventName = "mc_open_edit_screen")
        page.clickEditButton()
        page.clickSavedCardEditBadge(card1.last4)
        editPage.waitUntilVisible()

        networkRule.setupPaymentMethodDetachResponse(card1.id)
        testContext.validateAnalyticsRequest(eventName = "stripe_android.detach_payment_method")
        testContext.validateAnalyticsRequest(eventName = "mc_cancel_edit_screen")

        testContext.validateAnalyticsRequest(eventName = "mc_custom_paymentoption_removed")
        editPage.clickRemove()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.RemovedSavedPaymentMethod("card"))

        testContext.validateAnalyticsRequest(eventName = "mc_custom_paymentoption_savedpm_select")
        page.clickDoneButton()
        page.clickSavedCard(card2.last4)
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedSavedPaymentMethod("card"))
        testContext.markTestSucceeded()
    }

    private fun PaymentSheetTestRunnerContext.validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("PaymentSheet"),
            *requestMatchers
        )
    }

    private fun FlowControllerTestRunnerContext.validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("PaymentSheet.FlowController"),
            *requestMatchers
        )
    }
}
