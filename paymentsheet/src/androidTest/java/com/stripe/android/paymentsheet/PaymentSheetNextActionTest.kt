package com.stripe.android.paymentsheet

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.getIntents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.paymentsheet.utils.ApiConfigurationTestType
import com.stripe.android.paymentsheet.utils.ApiConfigurationTestTypeProvider
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.TestApiKeys
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
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

@RunWith(TestParameterInjector::class)
internal class PaymentSheetNextActionTest(
    @TestParameter(valuesProvider = ApiConfigurationTestTypeProvider::class)
    private val apiConfigurationTestType: ApiConfigurationTestType,
) {
    // The retrieve happens after the sheet has already handed back its result, so give
    // validate() a moment to see it.
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)

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

    @Test
    fun deferredPaymentIntent3ds2(
        @TestParameter tokenCallback: Boolean,
        @TestParameter serverConfirmed: Boolean,
        @TestParameter clearAccount: Boolean,
        @TestParameter fallback: Boolean,
    ) = runDeferred3ds2Test(tokenCallback, serverConfirmed, clearAccount, fallback, false, false)

    @Test
    fun deferredSetupIntent3ds2(
        @TestParameter tokenCallback: Boolean,
        @TestParameter fallback: Boolean,
    ) = runDeferred3ds2Test(tokenCallback, false, true, fallback, true, false)

    @Test
    fun nextActionPublishableKeyRetainsPrecedence() = runDeferred3ds2Test(
        tokenCallback = false,
        serverConfirmed = false,
        clearAccount = false,
        fallback = false,
        setup = false,
        nextActionKey = true,
    )

    private fun runDeferred3ds2Test(
        tokenCallback: Boolean,
        serverConfirmed: Boolean,
        clearAccount: Boolean,
        fallback: Boolean,
        setup: Boolean,
        nextActionKey: Boolean,
    ) {
        val clientSecret = if (setup) "seti_12345_secret_12345" else "pi_example_secret_example"
        val intentPath = if (setup) "/v1/setup_intents/seti_12345" else "/v1/payment_intents/pi_example"
        val returnUrl = DefaultReturnUrl.create(ApplicationProvider.getApplicationContext()).value
        val fallbackHtml = "<html><script>window.location.href='$returnUrl';</script></html>"
        val fallbackUrl = "data:text/html;base64," + Base64.encodeToString(fallbackHtml.toByteArray(), Base64.NO_WRAP)
        val account = "acct_override".takeUnless { clearAccount }
        val override = ApiConfiguration(OVERRIDE_KEY).stripeAccountId(account)
        val accountMatcher = RequestMatcher { request -> request.headers["Stripe-Account"] == account }
        runPaymentSheetTest(
            apiConfigurationTestType = apiConfigurationTestType,
            networkRule = networkRule,
            composeTestRule = testRules.compose,
            successTimeoutSeconds = 20L,
            resultCallback = ::assertCompleted,
            builder = {
                if (tokenCallback) {
                    createIntentCallback(CreateIntentWithConfirmationTokenCallback {
                        CreateIntentResult.Success(clientSecret).apiConfiguration(override)
                    })
                } else {
                    createIntentCallback(CreateIntentCallback { _, _ ->
                        CreateIntentResult.Success(clientSecret).apiConfiguration(override)
                    })
                }
            },
        ) { context ->
            networkRule.elementsSession { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            context.presentPaymentSheet {
                presentWithIntentConfiguration(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = if (setup) PaymentSheet.IntentConfiguration.Mode.Setup(currency = "usd")
                        else PaymentSheet.IntentConfiguration.Mode.Payment(amount = 5099, currency = "usd"),
                    ),
                    configuration = apiConfigurationTestType.applyTo(
                        PaymentSheet.Configuration(merchantDisplayName = "Example, Inc.")
                    ),
                )
            }
            page.fillOutCardDetails()
            networkRule.enqueue(
                method("POST"),
                path(if (tokenCallback) "/v1/confirmation_tokens" else "/v1/payment_methods"),
                header("Authorization", "Bearer ${TestApiKeys.PUBLISHABLE}"),
                header("Stripe-Account", TestApiKeys.ACCOUNT),
                applyDefaultAuthorization = false,
            ) { response ->
                response.testBodyFromFile(
                    if (tokenCallback) "confirmation-token-create-with-new-card.json" else "payment-methods-create.json"
                )
            }
            networkRule.enqueue(
                method("GET"), path(intentPath), header("Authorization", "Bearer $OVERRIDE_KEY"), accountMatcher,
                applyDefaultAuthorization = false,
            ) { response ->
                response.testBodyFromFile("payment-intent-3ds2.json") { json ->
                    configureIntent(json, setup, if (serverConfirmed) "requires_action" else "requires_confirmation")
                    if (!serverConfirmed) json.put("next_action", JSONObject.NULL)
                }
            }
            if (!serverConfirmed) {
                networkRule.enqueue(
                    method("POST"), path("$intentPath/confirm"),
                    header("Authorization", "Bearer $OVERRIDE_KEY"), accountMatcher,
                    applyDefaultAuthorization = false,
                ) { response ->
                    response.testBodyFromFile("payment-intent-3ds2.json") { json ->
                        configureIntent(json, setup, "requires_action")
                        if (nextActionKey) {
                            json.getJSONObject("next_action").getJSONObject("use_stripe_sdk")
                                .put("publishable_key", NEXT_ACTION_KEY)
                        }
                    }
                }
            }
            networkRule.enqueue(
                method("POST"), path("/v1/3ds2/authenticate"),
                header("Authorization", "Bearer ${if (nextActionKey) NEXT_ACTION_KEY else OVERRIDE_KEY}"),
                if (nextActionKey) RequestMatcher { it.headers["Stripe-Account"] == null } else accountMatcher,
                applyDefaultAuthorization = false,
            ) { response ->
                response.testBodyFromFile("3ds2-authenticate.json") { json ->
                    if (fallback) {
                        json.remove("ares")
                        json.put("fallback_redirect_url", fallbackUrl)
                    }
                }
            }
            networkRule.enqueue(
                method("GET"), path(intentPath), header("Authorization", "Bearer $OVERRIDE_KEY"), accountMatcher,
                applyDefaultAuthorization = false,
            ) { response ->
                response.testBodyFromFile("payment-intent-3ds2.json") { json ->
                    configureIntent(json, setup, "succeeded")
                    json.put("next_action", JSONObject.NULL)
                }
            }
            page.clickPrimaryButtonWithoutWaitingForDismissal()
            if (fallback) {
                testRules.compose.waitUntil(timeoutMillis = 10_000) {
                    getIntents().any { hasComponent(PAYMENT_AUTH_WEB_VIEW_ACTIVITY).matches(it) }
                }
            }
        }
    }

    private fun configureIntent(json: JSONObject, setup: Boolean, status: String) {
        json.put("status", status)
        if (setup) {
            json.put("id", "seti_12345")
            json.put("object", "setup_intent")
            json.put("client_secret", "seti_12345_secret_12345")
            json.put("usage", "off_session")
        }
    }

    private fun runNextActionTest(
        retrievedStatus: String,
        resultCallback: PaymentSheetResultCallback,
        afterConfirm: (PaymentSheetTestRunnerContext) -> Unit = {},
    ) = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        composeTestRule = testRules.compose,
        successTimeoutSeconds = 10L,
        resultCallback = resultCallback,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = apiConfigurationTestType.applyTo(
                    PaymentSheet.Configuration(
                        merchantDisplayName = "Example, Inc.",
                        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                    )
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

        page.clickPrimaryButtonWithoutWaitingForDismissal()

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
        const val PAYMENT_AUTH_WEB_VIEW_ACTIVITY = "com.stripe.android.view.PaymentAuthWebViewActivity"

        const val STRIPE_BROWSER_LAUNCHER_ACTIVITY =
            "com.stripe.android.payments.StripeBrowserLauncherActivity"

        const val OVERRIDE_KEY = "pk_test_override"
        const val NEXT_ACTION_KEY = "pk_test_next_action"

        const val AUTH_URL = "https://hooks.stripe.com/redirect/authenticate/src_1234"
    }
}
