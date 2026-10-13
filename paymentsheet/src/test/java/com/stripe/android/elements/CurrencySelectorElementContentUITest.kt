package com.stripe.android.elements

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_CURRENCY_OPTION_PREFIX
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_CURRENCY_SELECTOR
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CurrencySelectorElementContentUITest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val networkRule = NetworkRule()

    // Destroys built controllers when the test finishes, releasing each one's viewModelScope.
    private val destroyControllerRule = CleanupTestRule(CheckoutController::destroy)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        // Keep controller continuations on the test thread and let runTest drive Main work.
        .outerRule(CoroutineTestRule(StandardTestDispatcher()))
        .around(destroyControllerRule)
        .around(createComposeCleanupRule())
        .around(composeRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun whenAdaptivePricingInfoIsAbsent_currencySelectorDoesNotRender() = runScenario(
        fixture = FIXTURE_WITHOUT_ADAPTIVE_PRICING,
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
    fun whenAdaptivePricingInfoIsPresent_currencySelectorContentHasAccessibleLabels() = runScenario {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR")
            .assertContentDescriptionContains("45.94")

        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD")
            .assertContentDescriptionContains("50.99")
    }

    @Test
    fun currencySelectorConfiguredWithCurrencyCodeLabel_displaysCurrencyCodes() = runScenario(
        configuration = CheckoutController.Configuration().currencySelectorElement(
            CurrencySelectorElement.Configuration().appearance(
                CurrencySelectorElement.Configuration.Appearance()
                    .labelContent(CurrencySelectorElement.Configuration.Appearance.LabelContent.CURRENCY_CODE)
            )
        ),
    ) {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD")
            .assertContentDescriptionContains("USD")

        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR")
            .assertContentDescriptionContains("EUR")
    }

    @Test
    fun reconfiguringWithCurrencyCodeAfterComposition_updatesRenderedLabels() = runScenario {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD")
            .assertContentDescriptionContains("50.99")

        enqueueCheckoutInit()
        controller.configure(
            DEFAULT_CLIENT_SECRET,
            CheckoutController.Configuration().currencySelectorElement(
                CurrencySelectorElement.Configuration().appearance(
                    CurrencySelectorElement.Configuration.Appearance()
                        .labelContent(CurrencySelectorElement.Configuration.Appearance.LabelContent.CURRENCY_CODE)
                )
            ),
        ).getOrThrow()

        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD")
            .assertContentDescriptionContains("USD")
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR")
            .assertContentDescriptionContains("EUR")
    }

    @Test
    fun clickingSelectedOption_doesNotTriggerNetworkRequest() = runScenario {
        // EUR is the selected option. Clicking it is a no-op.
        // If it triggered a network request, NetworkRule teardown would fail
        // because no response is enqueued.
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR").performClick()
    }

    @Test
    fun clickingUnselectedOption_sendsUpdateCurrencyRequest() = runScenario {
        assertThat(controller.session.value?.currencySelectorOptions?.selectedCode)
            .isEqualTo("EUR")

        // The bodyPart matcher asserts the request carried updated_currency=USD; a request that
        // doesn't match won't be served, so receiving the event also proves the currency was correct.
        val requestReceived = Turbine<Unit>()
        networkRule.checkoutUpdate(
            bodyPart("updated_currency", "USD"),
            responseFactory = { response ->
                requestReceived.add(Unit)
                response.testBodyFromFile(FIXTURE_WITH_ADAPTIVE_PRICING) { json ->
                    json.put("customer_email", "checkout@example.com")
                    json.getJSONObject("elements_session").remove("link_settings")
                    json.getJSONObject("adaptive_pricing_info")
                        .put("active_presentment_currency", "usd")
                }
            },
        )

        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD").performClick()

        requestReceived.awaitItem()
        requestReceived.ensureAllEventsConsumed()
    }

    private fun runScenario(
        fixture: String = FIXTURE_WITH_ADAPTIVE_PRICING,
        setContent: Boolean = true,
        configuration: CheckoutController.Configuration = CheckoutController.Configuration()
            .currencySelectorElement(
                CurrencySelectorElement.Configuration()
            ),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        enqueueCheckoutInit(fixture)

        val controller = destroyControllerRule.track(
            CheckoutController.Builder(
                application = applicationContext,
                savedStateHandle = SavedStateHandle(),
            ).build()
        )

        controller.configure(DEFAULT_CLIENT_SECRET, configuration).getOrThrow()

        val element = controller.createPresenter(composeRule.activity).currencySelectorElement()
        if (setContent) {
            composeRule.setContent {
                element.Content()
            }
        }

        Scenario(controller = controller).block()
    }

    private fun enqueueCheckoutInit(fixture: String = FIXTURE_WITH_ADAPTIVE_PRICING) {
        networkRule.checkoutInit { response ->
            // The loader requires a billing email (sourced from customer_email) and Link is disabled
            // so the loader doesn't fire an unrelated consumer session lookup.
            response.testBodyFromFile(fixture) { json ->
                json.put("customer_email", "checkout@example.com")
                json.getJSONObject("elements_session").remove("link_settings")
            }
        }
    }

    private class Scenario(
        val controller: CheckoutController,
    )

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val FIXTURE_WITH_ADAPTIVE_PRICING = "checkout-session-adaptive-pricing-default.json"
        const val FIXTURE_WITHOUT_ADAPTIVE_PRICING = "checkout-session-init.json"
    }
}

private fun SemanticsNodeInteraction.assertContentDescriptionContains(value: String): SemanticsNodeInteraction {
    return assert(
        SemanticsMatcher("ContentDescription contains '$value'") { node ->
            node.config.contains(SemanticsProperties.ContentDescription) &&
                node.config[SemanticsProperties.ContentDescription].any {
                    it.contains(value)
                }
        }
    )
}
