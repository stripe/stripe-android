package com.stripe.android.checkout

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
import com.google.common.truth.Truth.assertThat
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
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CurrencySelectorElementContentUITest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val networkRule = NetworkRule()

    // Destroys built controllers when the test finishes, releasing each one's viewModelScope.
    private val destroyControllerRule = CleanupTestRule(CheckoutController::destroy)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(destroyControllerRule)
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
    fun clickingSelectedOption_doesNotTriggerNetworkRequest() = runScenario {
        // EUR is the selected option. Clicking it is a no-op.
        // If it triggered a network request, NetworkRule teardown would fail
        // because no response is enqueued.
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR").performClick()
    }

    @Test
    fun clickingUnselectedOption_sendsUpdateCurrencyRequest() = runScenario {
        assertThat(controller.checkoutSession.value?.currencySelectorOptions?.selectedCode)
            .isEqualTo("EUR")

        // The bodyPart matcher asserts the request carried updated_currency=USD; a request that
        // doesn't match won't be served, so a satisfied latch also proves the currency was correct.
        // The resulting session mutation runs through a real background dispatcher, so asserting the
        // updated session state here would require a wall-clock wait; that path is covered
        // deterministically by CheckoutControllerTest and CurrencySelectorViewModelTest instead.
        val requestReceived = CountDownLatch(1)
        networkRule.checkoutUpdate(
            bodyPart("updated_currency", "USD"),
            responseFactory = { response ->
                requestReceived.countDown()
                response.testBodyFromFile(FIXTURE_WITH_ADAPTIVE_PRICING) { json ->
                    json.put("customer_email", "checkout@example.com")
                    json.getJSONObject("elements_session").remove("link_settings")
                    json.getJSONObject("adaptive_pricing_info")
                        .put("active_presentment_currency", "usd")
                }
            },
        )

        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD").performClick()

        assertThat(requestReceived.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    private fun runScenario(
        fixture: String = FIXTURE_WITH_ADAPTIVE_PRICING,
        setContent: Boolean = true,
        block: Scenario.() -> Unit,
    ) {
        networkRule.checkoutInit { response ->
            // The loader requires a billing email (sourced from customer_email) and Link is disabled
            // so the loader doesn't fire an unrelated consumer session lookup.
            response.testBodyFromFile(fixture) { json ->
                json.put("customer_email", "checkout@example.com")
                json.getJSONObject("elements_session").remove("link_settings")
            }
        }

        val controller = destroyControllerRule.track(
            CheckoutController.Builder(
                application = applicationContext,
                savedStateHandle = SavedStateHandle(),
            ).build()
        )

        runBlocking { controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow() }

        // createPresenter registers an ActivityResultLauncher, which must happen before the activity
        // is STARTED. The compose rule's activity is already RESUMED, so build a CREATED activity for
        // presenter creation (the currency selector Content is still rendered by the compose rule).
        val presenterActivity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        val element = controller.createPresenter(presenterActivity).currencySelectorElement()
        if (setContent) {
            composeRule.setContent {
                element.Content()
            }
        }

        Scenario(controller = controller).block()
    }

    private class Scenario(
        val controller: CheckoutController,
    )

    private companion object {
        const val REQUEST_TIMEOUT_SECONDS = 5L
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
