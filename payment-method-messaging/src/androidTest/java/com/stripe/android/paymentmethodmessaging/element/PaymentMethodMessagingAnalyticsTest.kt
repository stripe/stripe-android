@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.testing.RetryRule
import com.stripe.android.testing.ShampooRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import kotlin.time.Duration.Companion.seconds

internal class PaymentMethodMessagingElementAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(AnalyticsRequest.HOST, "https://ppm.stripe.com"),
        validationTimeout = 1.seconds
    )

    private val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testRule: RuleChain = RuleChain.emptyRuleChain()
        .around(composeTestRule)
        .around(networkRule)
        .around(ShampooRule(500))
        .around(RetryRule(5))
        .around(AdvancedFraudSignalsTestRule())

    @Test
    fun testNoContent() = runPaymentMethodMessageAnalyticTest(networkRule = networkRule) { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("no-content.json")
        }

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_started",
            query("amount", "0"),
            query("currency", "usd"),
            query("locale", "en"),
            query("payment_methods", Uri.encode("klarna,affirm,afterpay_clearpay")),
            query("country_code", "US")
        )

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_succeeded",
            query("payment_methods", ""),
            query("content_type", "no_content")
        )

        testContext.configure(
            PaymentMethodMessagingElement.Configuration()
                .amount(0)
                .currency("usd")
                .locale("en")
                .countryCode("US")
                .paymentMethodTypes(
                    listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm, PaymentMethod.Type.AfterpayClearpay)
                )
        )
    }

    @Test
    fun testSinglePartner() = runPaymentMethodMessageAnalyticTest(networkRule = networkRule) { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("single-partner.json")
        }

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_started",
            query("amount", "5000"),
            query("currency", "usd"),
            query("locale", "en"),
            query("payment_methods", Uri.encode("klarna,affirm,afterpay_clearpay")),
            query("country_code", "US")
        )

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_succeeded",
            query("payment_methods", "klarna"),
            query("content_type", "single_partner")
        )

        testContext.configure(
            PaymentMethodMessagingElement.Configuration()
                .amount(5000)
                .currency("usd")
                .locale("en")
                .countryCode("US")
                .paymentMethodTypes(
                    listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm, PaymentMethod.Type.AfterpayClearpay)
                )
        )
    }

    @Test
    fun testMultiPartner() = runPaymentMethodMessageAnalyticTest(networkRule = networkRule) { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("multi-partner.json")
        }

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_started",
            query("amount", "5000"),
            query("currency", "usd"),
            query("locale", "en"),
            query("payment_methods", Uri.encode("klarna,affirm,afterpay_clearpay")),
            query("country_code", "US")
        )

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_succeeded",
            query("payment_methods", Uri.encode("klarna,affirm,afterpay_clearpay")),
            query("content_type", "multi_partner")
        )

        testContext.configure(
            PaymentMethodMessagingElement.Configuration()
                .amount(5000)
                .currency("usd")
                .locale("en")
                .countryCode("US")
                .paymentMethodTypes(
                    listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm, PaymentMethod.Type.AfterpayClearpay)
                )
        )
    }

    @Test
    fun testLoadFailed() = runPaymentMethodMessageAnalyticTest(networkRule = networkRule) { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.setResponseCode(400)
            response.testBodyFromFile("error-invalid-currency.json")
        }

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_started",
            query("amount", "0"),
            query("currency", "gel"),
            query("locale", "en"),
            query("payment_methods", Uri.encode("klarna,affirm,afterpay_clearpay")),
            query("country_code", "US")
        )

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_failed",
            query("error_message", "unsupported_currency"),
        )

        testContext.configure(
            PaymentMethodMessagingElement.Configuration()
                .amount(0)
                .currency("gel")
                .locale("en")
                .countryCode("US")
                .paymentMethodTypes(
                    listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm, PaymentMethod.Type.AfterpayClearpay)
                )
        )
    }

    @Test
    fun testElementTapped() = runPaymentMethodMessageAnalyticTest(networkRule = networkRule) { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("multi-partner.json")
        }

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_started",
            query("amount", "0"),
            query("currency", "gel"),
            query("locale", "en"),
            query("payment_methods", Uri.encode("klarna,affirm,afterpay_clearpay")),
            query("country_code", "US")
        )

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_succeeded",
            query("payment_methods", Uri.encode("klarna,affirm,afterpay_clearpay")),
            query("content_type", "multi_partner")
        )

        testContext.configure(
            PaymentMethodMessagingElement.Configuration()
                .amount(0)
                .currency("gel")
                .locale("en")
                .countryCode("US")
                .paymentMethodTypes(
                    listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm, PaymentMethod.Type.AfterpayClearpay)
                )
        )

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_tapped"
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(text = "4 interest-free payments", substring = true).performClick()
    }

    @Test
    fun testUnexpectedError() = runPaymentMethodMessageAnalyticTest(networkRule = networkRule) { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.setBody("{}")
        }

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_started",
            query("amount", "0"),
            query("currency", "gel"),
            query("locale", "en"),
            query("payment_methods", Uri.encode("klarna,affirm,afterpay_clearpay")),
            query("country_code", "US")
        )

        networkRule.validateAnalyticsRequest(
            eventName = "unexpected_error.paymentmethodmessaging.element.unable_to_parse_response",
        )

        testContext.configure(
            PaymentMethodMessagingElement.Configuration()
                .amount(0)
                .currency("gel")
                .locale("en")
                .countryCode("US")
                .paymentMethodTypes(
                    listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm, PaymentMethod.Type.AfterpayClearpay)
                )
        )
    }

    private fun runPaymentMethodMessageAnalyticTest(
        networkRule: NetworkRule,
        block: suspend (PaymentMethodMessagingElementTestRunnerContext) -> Unit
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_init"
        )

        networkRule.validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_displayed"
        )

        runPaymentMethodMessagingElementTest(
            networkRule = networkRule,
            block = block
        )
    }

    private fun NetworkRule.validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        enqueue(
            host("q.stripe.com"),
            method("GET"),
            query("event", eventName),
            *requestMatchers,
        ) { response ->
            response.status = "HTTP/1.1 200 OK"
        }
    }
}
