package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.Stripe
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.CustomerSheetTestType
import com.stripe.android.paymentsheet.utils.CustomerSheetUtils
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.runCustomerSheetTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal class CustomerSheetAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 1.seconds, // Analytics requests happen async.
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val composeTestRule = testRules.compose

    private val page: CustomerSheetPage = CustomerSheetPage(composeTestRule)

    @Before
    fun setup() {
        Stripe.advancedFraudSignalsEnabled = false
    }

    @After
    fun teardown() {
        Stripe.advancedFraudSignalsEnabled = true
    }

    @Test
    fun testSuccessfulCardSave() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = IntegrationType.Compose,
        customerSheetTestType = CustomerSheetTestType.AttachToSetupIntent,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)
        }
    ) { context ->
        networkRule.enqueue(
            CustomerSheetUtils.retrieveElementsSessionRequest(),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = false)

        validateAnalyticsRequest(eventName = "cs_init_with_customer_adapter")

        // These are all fired twice, once for cards & once for US bank account
        validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")
        validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")

        validateAnalyticsRequest(eventName = "elements.customer_sheet.elements_session.load_success")
        validateAnalyticsRequest(eventName = "elements.customer_sheet.payment_methods.load_success")

        validateAnalyticsRequest(eventName = "stripe_android.card_metadata_pk_available")
        validateAnalyticsRequest(eventName = "cs_add_payment_method_screen_presented")

        context.presentCustomerSheet()

        validateAnalyticsRequest(eventName = "cs_card_number_completed")

        page.fillOutCardDetails()

        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(),
            billingDetailsParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = true)
        CustomerSheetUtils.enqueueAttachRequests(
            networkRule = networkRule,
            customerSheetTestType = CustomerSheetTestType.AttachToSetupIntent,
        )

        validateAnalyticsRequest(eventName = "stripe_android.payment_method_creation")
        validateAnalyticsRequest(eventName = "stripe_android.setup_intent_retrieval")
        validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.started",
            query("intent_id", "seti_12345"),
        )
        validateAnalyticsRequest(eventName = "stripe_android.confirm_returnurl_null")
        validateAnalyticsRequest(eventName = "stripe_android.setup_intent_confirmation")
        validateAnalyticsRequest(
            eventName = "stripe_android.paymenthandler.confirm.finished",
            query("intent_id", "seti_12345"),
        )
        validateAnalyticsRequest(eventName = "cs_add_payment_method_via_setup_intent_success")

        // These are all fired twice, once for cards & once for US bank account
        validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        validateAnalyticsRequest(eventName = "stripe_android.retrieve_payment_methods")
        validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")
        validateAnalyticsRequest(eventName = "elements.customer_repository.get_saved_payment_methods_success")

        validateAnalyticsRequest(eventName = "elements.customer_sheet.payment_methods.refresh_success")
        validateAnalyticsRequest(eventName = "cs_select_payment_method_screen_presented")

        page.clickSaveButton()

        validateAnalyticsRequest(eventName = "cs_select_payment_method_screen_confirmed_savedpm_success")

        page.clickConfirmButton()
    }

    private fun validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("CustomerSheet"),
            requestMatchers = requestMatchers,
        )
    }
}
