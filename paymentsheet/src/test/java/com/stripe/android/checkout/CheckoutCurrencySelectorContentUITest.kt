package com.stripe.android.checkout

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_CURRENCY_OPTION_PREFIX
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_CURRENCY_SELECTOR
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutCurrencySelectorContentUITest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CheckoutInstancesTestRule())

    @Test
    fun whenAdaptivePricingInfoIsNull_currencySelectorDoesNotRender() = runScenario(
        adaptivePricingInfo = null,
    ) {
        composeRule.onNodeWithTag(TEST_TAG_CURRENCY_SELECTOR).assertDoesNotExist()
    }

    @Test
    fun whenAdaptivePricingInfoIsPresent_currencySelectorRenders() = runScenario {
        composeRule.onNodeWithTag(TEST_TAG_CURRENCY_SELECTOR).assertExists()
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD").assertExists()
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR").assertExists()
    }

    @Test
    fun clickingSelectedOption_doesNotTriggerNetworkRequest() = runScenario {
        // EUR is the selected option. Clicking it is a no-op.
        // If it triggered a network request, NetworkRule teardown would fail
        // because no response is enqueued.
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR").performClick()
    }

    private fun runScenario(
        adaptivePricingInfo: CheckoutSessionResponse.AdaptivePricingInfo? = DEFAULT_ADAPTIVE_PRICING_INFO,
        setContent: Boolean = true,
        block: suspend () -> Unit,
    ) {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
        )
        val state = CheckoutStateFactory.create(
            checkoutSessionResponse = checkoutSessionResponse,
        )
        val checkout = Checkout.createWithState(applicationContext, state)

        runTest {
            turbineScope {
                val checkoutSessionTurbine = checkout.checkoutSession.testIn(backgroundScope)

                // Consume initialization.
                assertThat(checkoutSessionTurbine.awaitItem()).isNotNull()

                if (setContent) {
                    composeRule.setContent {
                        checkout.CurrencySelectorContent()
                    }
                    composeRule.waitForIdle()
                }

                block()
            }
        }
    }

    companion object {
        private val DEFAULT_ADAPTIVE_PRICING_INFO = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "eur",
            integrationAmount = 5099,
            integrationCurrency = "usd",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 4594,
                    conversionMarkupBps = 400,
                    currency = "eur",
                    presentmentExchangeRate = "0.900961",
                ),
            ),
        )
    }
}
