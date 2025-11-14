@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
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

    private val page = PaymentMethodMessagingElementPage(composeTestRule)

    @get:Rule
    val testRule: RuleChain = RuleChain.emptyRuleChain()
        .around(composeTestRule)
        .around(networkRule)
        .around(RetryRule(5))
        .around(AdvancedFraudSignalsTestRule())

    @Test
    fun testNoContent() = runPaymentMethodMessagingElementTest { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("no-content.json")
        }

        val result = testContext.configure()
        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.NoContent::class.java)
        composeTestRule.onNodeWithContentDescription("Learn more").assertDoesNotExist()
    }

    @Test
    fun testSinglePartner() = runPaymentMethodMessagingElementTest { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("single-partner.json")
        }

        val result = testContext.configure()

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
        page.verifySinglePartner()
        page.openAndCloseLearnMoreActivity()
        composeTestRule.onNodeWithText("single partner inline_partner_promotion", substring = true).assertExists()
    }

    @Test
    fun testMultiPartner() = runPaymentMethodMessagingElementTest { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("multi-partner.json")
        }

        val result = testContext.configure()

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
        page.verifyMultiPartner()
        page.openAndCloseLearnMoreActivity()
    }

    @Test
    fun testError() = runPaymentMethodMessagingElementTest { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.setResponseCode(400)
            response.testBodyFromFile("error-invalid-currency.json")
        }

        val result = testContext.configure()

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Failed::class.java)
        assertThat(
            (result as PaymentMethodMessagingElement.ConfigureResult.Failed).error.message
        ).isEqualTo("unsupported_currency")
        composeTestRule.onNodeWithContentDescription("Learn more").assertDoesNotExist()
    }

    @Test
    fun testUpdatesContentOnConfigChange() = runPaymentMethodMessagingElementTest { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("single-partner.json")
        }

        testContext.configure()

        page.verifySinglePartner()

        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("multi-partner.json")
        }

        testContext.configure()

        page.verifyMultiPartner()

        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("no-content.json")
        }

        testContext.configure()

        composeTestRule.onNodeWithText(
            text = "multi partner promotion",
            substring = true
        ).assertDoesNotExist()
        composeTestRule.onNodeWithText(
            text = "single partner inline_partner_promotion",
            substring = true
        ).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Learn more").assertDoesNotExist()
    }
}
