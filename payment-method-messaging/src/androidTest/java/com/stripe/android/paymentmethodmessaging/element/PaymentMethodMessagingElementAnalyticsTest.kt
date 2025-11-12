@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.testing.RetryRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.time.Duration.Companion.seconds

class AdvancedFraudSignalsTestRule : TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)
        Stripe.advancedFraudSignalsEnabled = false
    }

    override fun finished(description: Description?) {
        Stripe.advancedFraudSignalsEnabled = true
        super.finished(description)
    }
}


internal class PaymentMethodMessagingElementAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(AnalyticsRequest.HOST, "https://ppm.stripe.com"),
        validationTimeout = 1.seconds
    )

    private val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testRule = RuleChain.emptyRuleChain()
        .around(composeTestRule)
        .around(networkRule)
        //.around(RetryRule(5))
        .around(AdvancedFraudSignalsTestRule())

    @Test
    fun testDisplaySinglePartner() = runPaymentMethodMessagingElementTest(networkRule = networkRule) { testContext ->
        networkRule.enqueue(
            host("ppm.stripe.com"),
            method("GET"),
            path("/config"),
        ) { response ->
            response.testBodyFromFile("no-content.json")
        }

        validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_init"
        )

        validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_displayed"
        )

        validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_started",
            query("amount", "0"),
            query("currency", "usd"),
            query("locale", "en"),
        )

        validateAnalyticsRequest(
            eventName = "payment_method_messaging_element_load_succeeded",
            query("payment_methods", ""),
            query("content_type", "no_content")
        )

        testContext.configure(
            PaymentMethodMessagingElement.Configuration()
                .amount(0)
                .currency("usd")
                .locale("en")
        )
    }

    private fun validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            *requestMatchers
        )
    }

    internal fun NetworkRule.validateAnalyticsRequest(
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


@OptIn(ExperimentalCoroutinesApi::class)
internal fun runPaymentMethodMessagingElementTest(
    networkRule: NetworkRule,
    block: suspend (PaymentMethodMessagingElementTestRunnerContext) -> Unit
) {
    val application = ApplicationProvider.getApplicationContext<Application>()
    PaymentConfiguration.init(application, "pk_test_123")
    var element =  PaymentMethodMessagingElement.create(application)
    val factory: (ComponentActivity) -> PaymentMethodMessagingElement = {
        it.setContent {
            Column {
                element.Content()
            }
        }
        element
    }

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)

        lateinit var paymentMethodMessagingElement: PaymentMethodMessagingElement
        scenario.onActivity {
            paymentMethodMessagingElement = factory(it)
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        val testContext = PaymentMethodMessagingElementTestRunnerContext(
            paymentMethodMessagingElement = paymentMethodMessagingElement
        )

        runTest {
            block(testContext)
        }

        networkRule.validate()
    }
}

internal class PaymentMethodMessagingElementTestRunnerContext(
    val paymentMethodMessagingElement: PaymentMethodMessagingElement
) {
    suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration
    ) {
        paymentMethodMessagingElement.configure(configuration)
    }
}

class MainActivity : ComponentActivity()