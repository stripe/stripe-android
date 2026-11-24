package com.stripe.android.paymentelement

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentelement.embedded.linkinline.resetEmbeddedLinkInlineAuthRegistry
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.paymentelementtestpages.EmbeddedLinkInlineVerificationPage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Inline Link verification for a returning Link customer in the Embedded Payment Element, across `configure` calls.
 */
@RunWith(AndroidJUnit4::class)
internal class EmbeddedPaymentElementLinkInlineOtpTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(FeatureFlagTestRule(FeatureFlags.nativeLinkEnabled, isEnabled = true))
            .around(FeatureFlagTestRule(FeatureFlags.nativeLinkAttestationEnabled, isEnabled = false))
            .around(FeatureFlagTestRule(FeatureFlags.embeddedInlineLinkOtp, isEnabled = true))
    }

    private val inlineVerificationPage = EmbeddedLinkInlineVerificationPage(testRules.compose)
    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)

    @Before
    fun resetInlineLinkVerification() {
        // Codes sent by earlier tests in this process would otherwise keep the card from showing.
        resetEmbeddedLinkInlineAuthRegistry()
    }

    @Test
    fun cardKeepsItsAttemptAcrossConfigure() = runLinkInlineOtpTest { testContext ->
        enqueueLoadForLinkCustomer(lookupFile = LOOKUP_RETURNING_CUSTOMER)
        enqueueCodeSent()
        testContext.configureForLinkCustomer()
        inlineVerificationPage.waitUntilVisible()
        inlineVerificationPage.waitUntilCodeIsSent()
        embeddedContentPage.assertLpmIsMissing("link")

        // The same account stays loaded, so there's no lookup and no second code.
        networkRule.elementsSession { response ->
            response.testBodyFromFile(ELEMENTS_SESSION)
        }
        testContext.configureForLinkCustomer(amount = UPDATED_AMOUNT)

        inlineVerificationPage.waitUntilVisible()
        inlineVerificationPage.waitUntilCodeIsSent()
        testContext.markTestSucceeded()
    }

    @Test
    fun configureThatHidesLinkEndsTheAttempt() = runLinkInlineOtpTest { testContext ->
        enqueueLoadForLinkCustomer(lookupFile = LOOKUP_RETURNING_CUSTOMER)
        enqueueCodeSent()
        testContext.configureForLinkCustomer()
        inlineVerificationPage.waitUntilVisible()
        inlineVerificationPage.waitUntilCodeIsSent()

        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }
        testContext.configureForLinkCustomer(amount = UPDATED_AMOUNT)

        inlineVerificationPage.waitUntilMissing()
        embeddedContentPage.assertLpmIsMissing("link")
        testContext.markTestSucceeded()
    }

    @Test
    fun newLinkCustomerKeepsTheLinkRow() = runLinkInlineOtpTest { testContext ->
        enqueueLoadForLinkCustomer(lookupFile = "consumer-session-lookup-success.json")
        testContext.configureForLinkCustomer()

        embeddedContentPage.assertLpmIsEnabled("link", isEnabled = true)
        inlineVerificationPage.assertIsNotVisible()
        testContext.markTestSucceeded()
    }

    private fun runLinkInlineOtpTest(
        block: suspend (EmbeddedPaymentElementTestRunnerContext) -> Unit,
    ) = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ -> error("Nothing is confirmed.") },
        resultCallback = { error("Nothing is confirmed.") },
        block = block,
    )

    private fun enqueueLoadForLinkCustomer(lookupFile: String) {
        networkRule.elementsSession { response ->
            response.testBodyFromFile(ELEMENTS_SESSION)
        }
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
            bodyPart("email_address", EMAIL),
        ) { response ->
            response.testBodyFromFile(lookupFile)
        }
    }

    /**
     * The code the card sends once the element is on screen.
     */
    private fun enqueueCodeSent() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/start_verification"),
            bodyPart("type", "SMS"),
        ) { response ->
            response.testBodyFromFile(LOOKUP_RETURNING_CUSTOMER) { json ->
                json.getJSONObject("consumer_session").put(
                    "verification_sessions",
                    JSONArray().put(JSONObject().put("type", "sms").put("state", "started")),
                )
            }
        }
    }

    /**
     * @param amount Configuring again with a different amount reloads the element, rather than reusing the last load.
     */
    private suspend fun EmbeddedPaymentElementTestRunnerContext.configureForLinkCustomer(amount: Long = AMOUNT) {
        configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(amount = amount, currency = "USD"),
            ),
        ) {
            defaultBillingDetails(PaymentSheet.BillingDetails(email = EMAIL))
        }
    }

    private companion object {
        const val ELEMENTS_SESSION = "elements-sessions-requires_payment_method.json"
        const val LOOKUP_RETURNING_CUSTOMER = "consumer-session-lookup-exists-success.json"
        const val EMAIL = "test@stripe.com"
        const val AMOUNT = 5000L
        const val UPDATED_AMOUNT = 6000L
    }
}
