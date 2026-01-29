package com.stripe.android.paymentsheet

import androidx.test.espresso.intent.rule.IntentsRule
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.ConfirmationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * Tests for confirmation challenge, which includes both passive captcha (hcaptcha_token) and
 * attestation (android_verification_object.android_verification_token) in radar_options.
 */
internal class ConfirmationChallengeTest {
    private val testRules: TestRules = TestRules.create()
    private val passiveCaptchaFeatureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enablePassiveCaptcha,
        isEnabled = true
    )
    private val attestationFeatureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableAttestationOnIntentConfirmation,
        isEnabled = true
    )

    @get:Rule
    val rules: RuleChain = RuleChain.emptyRuleChain()
        .around(IntentsRule())
        .around(passiveCaptchaFeatureFlagRule)
        .around(attestationFeatureFlagRule)
        .around(testRules)

    private val networkRule = testRules.networkRule

    @Test
    fun newPaymentMethod_withBothChallengesEnabled_includesBothTokensInConfirmRequest() =
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupBothChallengesEnabledTest(testContext)
        }

    @Test
    fun newPaymentMethod_withOnlyPassiveCaptchaEnabled_includesOnlyHCaptchaTokenInConfirmRequest() {
        passiveCaptchaFeatureFlagRule.setEnabled(true)
        attestationFeatureFlagRule.setEnabled(false)
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupOnlyPassiveCaptchaEnabledTest(testContext)
        }
    }

    @Test
    fun newPaymentMethod_withOnlyAttestationEnabled_includesOnlyAttestationTokenInConfirmRequest() {
        attestationFeatureFlagRule.setEnabled(true)
        passiveCaptchaFeatureFlagRule.setEnabled(false)
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupOnlyAttestationEnabledTest(testContext)
        }
    }

    @Test
    fun paymentMethodCreation_withBothChallengesEnabled_includesBothTokensInCreateRequest() =
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
            builder = {
                ConfirmationType.DeferredClientSideConfirmation().createIntentCallback?.let {
                    createIntentCallback(it)
                }
            }
        ) { testContext ->
            setupBothChallengesEnabledDeferredTest(testContext)
        }

    @Test
    fun paymentMethodCreation_withOnlyPassiveCaptchaEnabled_includesOnlyHCaptchaTokenInCreateRequest() {
        passiveCaptchaFeatureFlagRule.setEnabled(true)
        attestationFeatureFlagRule.setEnabled(false)
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
            builder = {
                ConfirmationType.DeferredClientSideConfirmation().createIntentCallback?.let {
                    createIntentCallback(it)
                }
            }
        ) { testContext ->
            setupOnlyPassiveCaptchaEnabledDeferredTest(testContext)
        }
    }

    @Test
    fun paymentMethodCreation_withOnlyAttestationEnabled_includesOnlyAttestationTokenInCreateRequest() {
        attestationFeatureFlagRule.setEnabled(true)
        passiveCaptchaFeatureFlagRule.setEnabled(false)
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
            builder = {
                ConfirmationType.DeferredClientSideConfirmation().createIntentCallback?.let {
                    createIntentCallback(it)
                }
            }
        ) { testContext ->
            setupOnlyAttestationEnabledDeferredTest(testContext)
        }
    }

    private fun setupBothChallengesEnabledTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithBothChallengesEnabled(networkRule)

        testContext.launch()

        val paymentSheetPage = createPaymentSheetPage()
        paymentSheetPage.fillOutCardDetails()

        enqueuePaymentIntentConfirmWithBothTokens()
        paymentSheetPage.clickPrimaryButton()
    }

    private fun setupOnlyPassiveCaptchaEnabledTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithOnlyPassiveCaptchaEnabled(networkRule)

        testContext.launch()

        val paymentSheetPage = createPaymentSheetPage()
        paymentSheetPage.fillOutCardDetails()

        enqueuePaymentIntentConfirmWithOnlyHCaptchaToken()
        paymentSheetPage.clickPrimaryButton()
    }

    private fun setupOnlyAttestationEnabledTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithOnlyAttestationEnabled(networkRule)

        testContext.launch()

        val paymentSheetPage = createPaymentSheetPage()
        paymentSheetPage.fillOutCardDetails()

        enqueuePaymentIntentConfirmWithOnlyAttestationToken()
        paymentSheetPage.clickPrimaryButton()
    }

    private fun setupBothChallengesEnabledDeferredTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithBothChallengesEnabled(networkRule, isDeferredIntent = true)
        testContext.launch(isDeferredIntent = true)

        navigateToFormForLpm()

        enqueuePaymentMethodCreateWithBothTokens()
        enqueueDeferredIntentRequests()

        createPaymentSheetPage().clickPrimaryButton()
    }

    private fun setupOnlyPassiveCaptchaEnabledDeferredTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithOnlyPassiveCaptchaEnabled(networkRule, isDeferredIntent = true)
        testContext.launch(isDeferredIntent = true)

        navigateToFormForLpm()

        enqueuePaymentMethodCreateWithOnlyHCaptchaToken()
        enqueueDeferredIntentRequests()

        createPaymentSheetPage().clickPrimaryButton()
    }

    private fun setupOnlyAttestationEnabledDeferredTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithOnlyAttestationEnabled(networkRule, isDeferredIntent = true)
        testContext.launch(isDeferredIntent = true)

        navigateToFormForLpm()

        enqueuePaymentMethodCreateWithOnlyAttestationToken()
        enqueueDeferredIntentRequests()

        createPaymentSheetPage().clickPrimaryButton()
    }

    private fun navigateToFormForLpm() {
        val verticalModePage = createPaymentSheetPage()
        verticalModePage.waitUntilVisible()
        verticalModePage.fillOutCardDetails()
    }

    private fun createPaymentSheetPage() = PaymentSheetPage(testRules.compose)

    private fun enqueueElementsSessionWithBothChallengesEnabled(
        networkRule: NetworkRule,
        isDeferredIntent: Boolean = false
    ) {
        val replacement = ResponseReplacement(
            "\"unactivated_payment_method_types\": []",
            "\"unactivated_payment_method_types\": [],\n" +
                "  \"flags\": {\n" +
                "    \"elements_enable_passive_captcha\": true,\n" +
                "    \"elements_mobile_attest_on_intent_confirmation\": true\n" +
                "  },\n" +
                "  \"passive_captcha\": {\n" +
                "    \"site_key\": \"$HCAPTCHA_SITE_KEY\",\n" +
                "    \"rqdata\": null\n" +
                "  }"
        )

        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(
                if (isDeferredIntent) DEFERRED_INTENT_ELEMENTS_SESSION_FILE else STANDARD_ELEMENTS_SESSION_FILE,
                replacements = EMPTY_PAYMENT_METHODS_REPLACEMENTS + replacement
            )
        }
    }

    private fun enqueueElementsSessionWithOnlyPassiveCaptchaEnabled(
        networkRule: NetworkRule,
        isDeferredIntent: Boolean = false
    ) {
        val replacement = ResponseReplacement(
            "\"unactivated_payment_method_types\": []",
            "\"unactivated_payment_method_types\": [],\n" +
                "  \"flags\": {\n" +
                "    \"elements_enable_passive_captcha\": true\n" +
                "  },\n" +
                "  \"passive_captcha\": {\n" +
                "    \"site_key\": \"$HCAPTCHA_SITE_KEY\",\n" +
                "    \"rqdata\": null\n" +
                "  }"
        )

        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(
                if (isDeferredIntent) DEFERRED_INTENT_ELEMENTS_SESSION_FILE else STANDARD_ELEMENTS_SESSION_FILE,
                replacements = EMPTY_PAYMENT_METHODS_REPLACEMENTS + replacement
            )
        }
    }

    private fun enqueueElementsSessionWithOnlyAttestationEnabled(
        networkRule: NetworkRule,
        isDeferredIntent: Boolean = false
    ) {
        val replacement = ResponseReplacement(
            "\"unactivated_payment_method_types\": []",
            "\"unactivated_payment_method_types\": [],\n" +
                "  \"flags\": {\n" +
                "    \"elements_mobile_attest_on_intent_confirmation\": true\n" +
                "  }"
        )

        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(
                if (isDeferredIntent) DEFERRED_INTENT_ELEMENTS_SESSION_FILE else STANDARD_ELEMENTS_SESSION_FILE,
                replacements = EMPTY_PAYMENT_METHODS_REPLACEMENTS + replacement
            )
        }
    }

    private fun enqueuePaymentIntentConfirmWithBothTokens() {
        networkRule.enqueue(
            method("POST"),
            path(PAYMENT_INTENT_CONFIRM_PATH),
            bodyPart(urlEncode(NEW_PM_HCAPTCHA_TOKEN_PATH), HCAPTCHA_TOKEN),
            bodyPart(urlEncode(NEW_PM_ATTESTATION_TOKEN_PATH), Regex(".+")),
        ) { response ->
            response.testBodyFromFile(PAYMENT_INTENT_CONFIRM_FILE)
        }
    }

    private fun enqueuePaymentIntentConfirmWithOnlyHCaptchaToken() {
        networkRule.enqueue(
            method("POST"),
            path(PAYMENT_INTENT_CONFIRM_PATH),
            bodyPart(urlEncode(NEW_PM_HCAPTCHA_TOKEN_PATH), HCAPTCHA_TOKEN),
            not(bodyPart(urlEncode(NEW_PM_ATTESTATION_TOKEN_PATH), Regex(".+"))),
        ) { response ->
            response.testBodyFromFile(PAYMENT_INTENT_CONFIRM_FILE)
        }
    }

    private fun enqueuePaymentIntentConfirmWithOnlyAttestationToken() {
        networkRule.enqueue(
            method("POST"),
            path(PAYMENT_INTENT_CONFIRM_PATH),
            not(bodyPart(urlEncode(NEW_PM_HCAPTCHA_TOKEN_PATH), HCAPTCHA_TOKEN)),
            bodyPart(urlEncode(NEW_PM_ATTESTATION_TOKEN_PATH), Regex(".+")),
        ) { response ->
            response.testBodyFromFile(PAYMENT_INTENT_CONFIRM_FILE)
        }
    }

    private fun enqueuePaymentMethodCreateWithBothTokens() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(urlEncode(SAVED_PM_HCAPTCHA_TOKEN_PATH), HCAPTCHA_TOKEN),
            bodyPart(urlEncode(SAVED_PM_ATTESTATION_TOKEN_PATH), Regex(".+")),
        ) { response ->
            response.testBodyFromFile(PAYMENT_METHOD_CREATE_FILE)
        }
    }

    private fun enqueuePaymentMethodCreateWithOnlyHCaptchaToken() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(urlEncode(SAVED_PM_HCAPTCHA_TOKEN_PATH), HCAPTCHA_TOKEN),
            not(bodyPart(urlEncode(SAVED_PM_ATTESTATION_TOKEN_PATH), Regex(".+"))),
        ) { response ->
            response.testBodyFromFile(PAYMENT_METHOD_CREATE_FILE)
        }
    }

    private fun enqueuePaymentMethodCreateWithOnlyAttestationToken() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            not(bodyPart(urlEncode(SAVED_PM_HCAPTCHA_TOKEN_PATH), HCAPTCHA_TOKEN)),
            bodyPart(urlEncode(SAVED_PM_ATTESTATION_TOKEN_PATH), Regex(".+")),
        ) { response ->
            response.testBodyFromFile(PAYMENT_METHOD_CREATE_FILE)
        }
    }

    private fun enqueueDeferredIntentRequests() {
        networkRule.enqueue(
            method("GET"),
            path(PAYMENT_INTENT_GET_PATH),
        ) { response ->
            response.testBodyFromFile(PAYMENT_INTENT_GET_FILE)
        }

        networkRule.enqueue(
            method("POST"),
            path(PAYMENT_INTENT_CONFIRM_PATH),
        ) { response ->
            response.testBodyFromFile(PAYMENT_INTENT_CONFIRM_FILE)
        }
    }

    companion object {
        private const val HCAPTCHA_TOKEN = "20000000-aaaa-bbbb-cccc-000000000002"
        private const val HCAPTCHA_SITE_KEY = "20000000-ffff-ffff-ffff-000000000002"

        private const val NEW_PM_HCAPTCHA_TOKEN_PATH =
            "payment_method_data[radar_options][hcaptcha_token]"
        private const val NEW_PM_ATTESTATION_TOKEN_PATH =
            "payment_method_data[radar_options][android_verification_object][android_verification_token]"
        private const val SAVED_PM_HCAPTCHA_TOKEN_PATH =
            "radar_options[hcaptcha_token]"
        private const val SAVED_PM_ATTESTATION_TOKEN_PATH =
            "radar_options[android_verification_object][android_verification_token]"

        private const val STANDARD_ELEMENTS_SESSION_FILE =
            "elements-sessions-with_pi_and_default_pms_enabled.json"
        private const val DEFERRED_INTENT_ELEMENTS_SESSION_FILE =
            "elements-sessions-deferred_intent_and_default_pms_enabled.json"
        private const val PAYMENT_INTENT_CONFIRM_FILE = "payment-intent-confirm.json"
        private const val PAYMENT_METHOD_CREATE_FILE = "payment-methods-create.json"
        private const val PAYMENT_INTENT_GET_FILE = "payment-intent-get-requires_payment_method.json"

        private const val PAYMENT_INTENT_CONFIRM_PATH = "/v1/payment_intents/pi_example/confirm"
        private const val PAYMENT_INTENT_GET_PATH = "/v1/payment_intents/pi_example"

        private val EMPTY_PAYMENT_METHODS_REPLACEMENTS = listOf(
            ResponseReplacement("DEFAULT_PAYMENT_METHOD_HERE", "null"),
            ResponseReplacement("[PAYMENT_METHODS_HERE]", "[]")
        )
    }
}
