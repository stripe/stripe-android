@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement

import androidx.test.espresso.Espresso
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.stripe.android.Stripe
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupPaymentMethodDetachResponse
import com.stripe.paymentelementnetwork.setupV1PaymentMethodsResponse
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.ManagePage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
internal class EmbeddedPaymentElementAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 1.seconds, // Analytics requests happen async.
    )
    private val analyticEventRule = AnalyticEventRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(analyticEventRule)
    }

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)
    private val managePage = ManagePage(testRules.compose)
    private val editPage = EditPage(testRules.compose)

    private val card1 = CardPaymentMethodDetails("pm_12345", "4242")
    private val card2 = CardPaymentMethodDetails("pm_67890", "5544")

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
    fun testSuccessfulCardPayment() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
        analyticEventCallback = analyticEventRule,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        validateAnalyticsRequest(eventName = "mc_embedded_init")
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
        validateAnalyticsRequest(eventName = "mc_embedded_sheet_newpm_show")
        validateAnalyticsRequest(eventName = "mc_carousel_payment_method_tapped")
        validateAnalyticsRequest(eventName = "mc_form_shown")

        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        validateAnalyticsRequest(eventName = "mc_form_interacted")
        validateAnalyticsRequest(eventName = "mc_card_number_completed")

        testContext.configure()

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.CompletedPaymentMethodForm("card"))

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        validateAnalyticsRequest(
            eventName = "stripe_android.payment_method_creation",
            additionalProductUsage = setOf("deferred-intent", "autopm"),
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
        )
        validateAnalyticsRequest(eventName = "mc_confirm_button_tapped")
        validateAnalyticsRequest(eventName = "mc_embedded_payment_success")

        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
    }

    @Test
    fun testCheckoutWithSavedCard() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
        analyticEventCallback = analyticEventRule,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }
        networkRule.setupV1PaymentMethodsResponse(card1, card2)

        validateAnalyticsRequest(eventName = "mc_embedded_init")
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
        validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")
        validateAnalyticsRequest(eventName = "mc_embedded_sheet_newpm_show")

        testContext.configure {
            customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        embeddedContentPage.assertHasSelectedSavedPaymentMethod("pm_12345")

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

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
        )
        validateAnalyticsRequest(eventName = "mc_embedded_payment_success")

        testContext.confirm()
    }

    @Test
    fun testEditCard() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
        analyticEventCallback = analyticEventRule,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }
        networkRule.setupV1PaymentMethodsResponse(card1, card2)

        validateAnalyticsRequest(eventName = "mc_embedded_init")
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
        validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")
        validateAnalyticsRequest(eventName = "mc_embedded_sheet_newpm_show")

        testContext.configure {
            customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
        }

        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        validateAnalyticsRequest(eventName = "mc_embedded_manage_savedpm_show")
        embeddedContentPage.clickViewMore()

        managePage.waitUntilVisible()
        managePage.clickEdit()
        validateAnalyticsRequest(eventName = "mc_open_edit_screen")
        managePage.clickEdit(card1.id)
        editPage.waitUntilVisible()
        validateAnalyticsRequest(eventName = "mc_cancel_edit_screen")
        Espresso.pressBack()
        managePage.waitUntilVisible()
        managePage.clickDone()
        Espresso.pressBack()

        testContext.markTestSucceeded()
    }

    @Test
    fun testRemoveCard() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        analyticEventCallback = analyticEventRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }
        networkRule.setupV1PaymentMethodsResponse(card1, card2)

        validateAnalyticsRequest(eventName = "mc_embedded_init")
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
        validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")
        validateAnalyticsRequest(eventName = "mc_embedded_sheet_newpm_show")

        testContext.configure {
            customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
        }
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

        validateAnalyticsRequest(eventName = "mc_embedded_manage_savedpm_show")
        embeddedContentPage.clickViewMore()

        managePage.waitUntilVisible()
        managePage.clickEdit()
        validateAnalyticsRequest(eventName = "mc_open_edit_screen")
        managePage.clickEdit(card1.id)
        editPage.waitUntilVisible()

        networkRule.setupPaymentMethodDetachResponse(card1.id)
        validateAnalyticsRequest(eventName = "stripe_android.detach_payment_method")
        validateAnalyticsRequest(eventName = "mc_cancel_edit_screen")

        editPage.clickRemove()
        analyticEventRule.assertMatchesExpectedEvent(AnalyticEvent.RemovedSavedPaymentMethod("card"))

        managePage.waitUntilVisible()
        managePage.waitUntilGone(card1.id)
        managePage.clickDone()

        testContext.markTestSucceeded()
    }

    private fun validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
        additionalProductUsage: Set<String> = emptySet(),
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("EmbeddedPaymentElement").plus(additionalProductUsage),
            *requestMatchers
        )
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
