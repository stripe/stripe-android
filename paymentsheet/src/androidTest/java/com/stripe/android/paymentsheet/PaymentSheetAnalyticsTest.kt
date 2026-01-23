package com.stripe.android.paymentsheet

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventRule
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
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
@RunWith(AndroidJUnit4::class)
internal class PaymentSheetAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds, // Analytics requests happen async.
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

        validateAnalyticsRequest(eventName = "mc_complete_init")
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(
            eventName = "mc_load_succeeded",
            query(Uri.encode("mpe_config[analytic_callback_set]"), "true"),
        )
        validateAnalyticsRequest(eventName = "mc_complete_sheet_newpm_show")
        validateAnalyticsRequest(eventName = "mc_form_shown")
        // cardscan is not available in test mode
        validateAnalyticsRequest(eventName = "mc_cardscan_api_check_failed")
        validateAnalyticsRequest(
            eventName = "mc_initial_displayed_payment_methods",
            query("hidden_payment_methods", Uri.encode("cashapp,affirm,alipay,wechat_pay")),
            query("visible_payment_methods", Uri.encode("link,card,afterpay_clearpay,klarna")),
            query("payment_method_layout", "horizontal"),
        )
        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = horizontalModeConfiguration,
            )
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.DisplayedPaymentMethodForm("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        validateAnalyticsRequest(eventName = "mc_form_interacted")
        validateAnalyticsRequest(eventName = "mc_card_number_completed")

        validateAnalyticsRequest(eventName = "mc_form_completed")
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
        validateAnalyticsRequest(
            eventName = "mc_complete_payment_newpm_success",
            hasQueryParam("duration"),
            query("intent_id", "pi_example"),
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

        validateAnalyticsRequest(eventName = "mc_complete_init")
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
        validateAnalyticsRequest(eventName = "mc_complete_sheet_newpm_show")
        validateAnalyticsRequest(eventName = "mc_carousel_payment_method_tapped")
        validateAnalyticsRequest(eventName = "mc_form_shown")
        // cardscan is not available in test mode
        validateAnalyticsRequest(eventName = "mc_cardscan_api_check_failed")
        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        validateAnalyticsRequest(
            eventName = "mc_initial_displayed_payment_methods",
            query("visible_payment_methods", Uri.encode("link,card,afterpay_clearpay,klarna,cashapp,affirm,alipay,wechat_pay")),
            query("payment_method_layout", "vertical"),
        )

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = verticalModeConfiguration,
            )
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        validateAnalyticsRequest(eventName = "mc_form_interacted")
        validateAnalyticsRequest(eventName = "mc_card_number_completed")
        page.clickOnLpm("card", forVerticalMode = true)
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedPaymentMethodType("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.DisplayedPaymentMethodForm("card"))

        validateAnalyticsRequest(eventName = "mc_form_completed")
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
        validateAnalyticsRequest(
            eventName = "mc_complete_payment_newpm_success",
            hasQueryParam("duration")
        )

        page.clickPrimaryButton()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.TappedConfirmButton("card"))
    }

    @Test
    fun testSuccessfulCardPaymentWithConfirmationToken() = runPaymentSheetTest(
        networkRule = networkRule,
        builder = {
            createIntentCallback { _ ->
                CreateIntentResult.Success("pi_example_secret_example")
            }
            analyticEventCallback(analyticEventRule)
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        validateAnalyticsRequest(eventName = "mc_complete_init")
        validateAnalyticsRequest(eventName = "mc_load_started")
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        validateAnalyticsRequest(
            eventName = "mc_load_succeeded",
            query(Uri.encode("mpe_config[analytic_callback_set]"), "true"),
            query(Uri.encode("mpe_config[payment_method_layout]"), "horizontal"),
            query(Uri.encode("is_confirmation_tokens"), "true"),
            query(Uri.encode("is_decoupled"), "true"),
            query(Uri.encode("intent_type"), "deferred_payment_intent"),
        )

        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        validateAnalyticsRequest(eventName = "mc_complete_sheet_newpm_show")
        validateAnalyticsRequest(eventName = "mc_form_shown")
        // cardscan is not available in test mode
        validateAnalyticsRequest(eventName = "mc_cardscan_api_check_failed")
        validateAnalyticsRequest(eventName = "mc_initial_displayed_payment_methods")

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = horizontalModeConfiguration
            )
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.DisplayedPaymentMethodForm("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        validateAnalyticsRequest(eventName = "mc_form_interacted")
        validateAnalyticsRequest(eventName = "mc_card_number_completed")
        validateAnalyticsRequest(eventName = "mc_form_completed")
        page.fillOutCardDetails()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.StartedInteractionWithPaymentMethodForm("card"))
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.CompletedPaymentMethodForm("card"))

        networkRule.enqueue(
            method("POST"),
            path("/v1/confirmation_tokens"),
            clientAttributionMetadataParamsForDeferredIntent(),
        ) { response ->
            response.testBodyFromFile("confirmation-token-create-with-new-card.json")
        }
        networkRule.enqueue(
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart("confirmation_token", "ctoken_example"),
            bodyPart("return_url", urlEncode("stripesdk://payment_return_url/com.stripe.android.paymentsheet.test")),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
        validateAnalyticsRequest(eventName = "mc_confirm_button_tapped")
        networkRule.validateAnalyticsRequest(
            eventName = "stripe_android.confirmation_token_creation",
            productUsage = setOf("PaymentSheet", "deferred-intent", "autopm")
        )
        validateAnalyticsRequest(eventName = "stripe_android.payment_intent_retrieval")
        validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.started",
            query("intent_id", "pi_example"),
        )
        validateAnalyticsRequest(eventName = "stripe_android.confirm_returnurl_null")
        validateAnalyticsRequest(eventName = "stripe_android.payment_intent_confirmation")
        validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.finished",
            query("intent_id", "pi_example"),
            query("payment_method_type", "card"),
        )
        validateAnalyticsRequest(
            eventName = "mc_complete_payment_newpm_success",
            query("is_confirmation_tokens", "true"),
            query("intent_id", "pi_example"),
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
        validateAnalyticsRequest(eventName = "mc_complete_init")
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
        validateAnalyticsRequest(eventName = "mc_complete_sheet_savedpm_show")

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = horizontalModeConfiguration.newBuilder()
                    .customer(
                        PaymentSheet.CustomerConfiguration(
                            id = "cus_1",
                            ephemeralKeySecret = "ek_123",
                        )
                    )
                    .build()
            )
        }
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        validateAnalyticsRequest(eventName = "mc_open_edit_screen")
        page.clickEditButton()
        page.clickSavedCardEditBadge(card1.last4)
        editPage.waitUntilVisible()

        networkRule.setupPaymentMethodDetachResponse(card1.id)
        validateAnalyticsRequest(eventName = "stripe_android.detach_payment_method")
        validateAnalyticsRequest(eventName = "mc_cancel_edit_screen")

        validateAnalyticsRequest(eventName = "mc_complete_paymentoption_removed")
        editPage.clickRemove()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.RemovedSavedPaymentMethod("card"))

        validateAnalyticsRequest(eventName = "mc_complete_paymentoption_savedpm_select")
        page.clickDoneButton()
        page.clickSavedCard(card2.last4)
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedSavedPaymentMethod("card"))
        testContext.markTestSucceeded()
    }

    private fun validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("PaymentSheet"),
            *requestMatchers
        )
    }
}
