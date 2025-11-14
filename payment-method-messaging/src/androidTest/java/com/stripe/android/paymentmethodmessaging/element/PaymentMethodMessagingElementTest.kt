@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.testing.RetryRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import kotlin.time.Duration.Companion.seconds

class PaymentMethodMessagingElementTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf("https://ppm.stripe.com"),
        validationTimeout = 1.seconds
    )
    private val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testRule: RuleChain = RuleChain.emptyRuleChain()
        .around(composeTestRule)
        .around(networkRule)
        .around(RetryRule(5))
        .around(AdvancedFraudSignalsTestRule())

    @Test
    fun testNoContent() = runPaymentMethodMessagingElementTest(networkRule = networkRule) { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("no-content.json")
        }

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
    fun testSinglePartner() = runPaymentMethodMessagingElementTest(networkRule = networkRule) { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("single-partner.json")
        }

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

        composeTestRule.onNodeWithText("4 interest-free payments of ", substring = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Learn more").assertExists()
    }
}