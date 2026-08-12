package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.getIntents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.hamcrest.Matchers.allOf
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
internal class PaymentSheetNextActionTest {
    // The retrieve happens after the sheet has already handed back its result, so give
    // validate() a moment to see it.
    private val networkRule = NetworkRule(
        validationTimeout = 5.seconds,
        defaultMatcher = header("Authorization", "Bearer pk_test_123"),
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(IntentsRule())
    }

    private val page = PaymentSheetPage(testRules.compose)

    @Test
    fun testCardPaymentWithRedirectNextAction() = runNextActionTest(
        retrievedStatus = "succeeded",
        resultCallback = ::assertCompleted,
    )

    @Test
    fun testCardPaymentWithRedirectNextActionUnresolved() = runNextActionTest(
        retrievedStatus = "requires_action",
        resultCallback = ::expectNoResult,
    ) { testContext ->
        page.waitForText(
            "We are unable to authenticate your payment method. " +
                "Please choose a different payment method and try again.",
            substring = true,
        )
        testContext.markTestSucceeded()
    }

    private fun runNextActionTest(
        retrievedStatus: String,
        resultCallback: PaymentSheetResultCallback,
        afterConfirm: (PaymentSheetTestRunnerContext) -> Unit = {},
    ) = runPaymentSheetTest(
        networkRule = networkRule,
        successTimeoutSeconds = 10L,
        resultCallback = resultCallback,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Example, Inc.",
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                ),
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm_with-requires_action-status.json") { json ->
                json.put(
                    "next_action",
                    JSONObject(
                        """
                        {
                          "type": "redirect_to_url",
                          "redirect_to_url": {
                            "url": "$AUTH_URL",
                            "return_url": "${DefaultReturnUrl.create(
                            ApplicationProvider.getApplicationContext()
                        ).value}"
                          }
                        }
                        """.trimIndent()
                    )
                )
            }
        }

        // Let the real StripeBrowserLauncherActivity run, and only stand in for the browser tab it
        // opens. Responding unblocks its registerForActivityResult callback, the same way returning
        // from Chrome does.
        intending(allOf(hasAction(Intent.ACTION_VIEW), hasData(AUTH_URL))).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        )

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get.json") { json ->
                json.put("status", retrievedStatus)
            }
        }

        page.clickPrimaryButton()

        waitForBrowserAuthToLaunch()

        afterConfirm(testContext)
    }

    private fun waitForBrowserAuthToLaunch() {
        testRules.compose.waitUntil(timeoutMillis = 10_000) {
            val intents = getIntents()
            intents.any { hasComponent(STRIPE_BROWSER_LAUNCHER_ACTIVITY).matches(it) } &&
                intents.any { allOf(hasAction(Intent.ACTION_VIEW), hasData(AUTH_URL)).matches(it) }
        }
    }

    private companion object {
        const val STRIPE_BROWSER_LAUNCHER_ACTIVITY =
            "com.stripe.android.payments.StripeBrowserLauncherActivity"

        const val AUTH_URL = "https://hooks.stripe.com/redirect/authenticate/src_1234"
    }
}
