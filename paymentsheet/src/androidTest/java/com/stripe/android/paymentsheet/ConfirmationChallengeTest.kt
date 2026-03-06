package com.stripe.android.paymentsheet

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for confirmation challenge, which includes both passive captcha (hcaptcha_token) and
 * attestation (android_verification_object.android_verification_token) in radar_options.
 */
@RunWith(TestParameterInjector::class)
internal class ConfirmationChallengeTest {
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)
    private val testRules: TestRules = TestRules.create(networkRule = networkRule)
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

    @TestParameter(valuesProvider = ChallengeConfigProvider::class)
    lateinit var challengeConfig: ChallengeConfig

    /**
     * Tests that the correct challenge tokens are included in the payment intent confirm request
     * when using a new payment method with intent-first flow.
     */
    @Test
    fun newPaymentMethod_includesCorrectTokensInConfirmRequest() {
        challengeConfig.configureFeatureFlags(passiveCaptchaFeatureFlagRule, attestationFeatureFlagRule)
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupIntentFirstTest(testContext)
        }
    }

    /**
     * Tests that the correct challenge tokens are included in the payment method create request
     * when using deferred client-side confirmation with CreateIntentCallback.
     */
    @Test
    fun paymentMethodCreation_includesCorrectTokensInCreateRequest() {
        challengeConfig.configureFeatureFlags(passiveCaptchaFeatureFlagRule, attestationFeatureFlagRule)
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
            builder = {
                createIntentCallback { _, _ ->
                    CreateIntentResult.Success(clientSecret = "pi_example_secret_example")
                }
            }
        ) { testContext ->
            setupDeferredTest(testContext)
        }
    }

    /**
     * Tests that the correct challenge tokens are included in the confirmation token create request
     * when using deferred confirmation with CreateIntentWithConfirmationTokenCallback.
     */
    @Test
    fun confirmationToken_includesCorrectTokensInConfirmationTokenRequest() {
        challengeConfig.configureFeatureFlags(passiveCaptchaFeatureFlagRule, attestationFeatureFlagRule)
        runPaymentSheetTest(
            networkRule = networkRule,
            builder = {
                createIntentCallback(
                    callback = CreateIntentWithConfirmationTokenCallback { _ ->
                        CreateIntentResult.Success("pi_example_secret_example")
                    }
                )
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupConfirmationTokenTest(testContext)
        }
    }

    private fun setupIntentFirstTest(testContext: ProductIntegrationTestRunnerContext) {
        challengeConfig.enqueueElementsSession(networkRule, isDeferredIntent = false)

        testContext.launch()

        val paymentSheetPage = createPaymentSheetPage()
        paymentSheetPage.fillOutCardDetails()

        challengeConfig.enqueuePaymentIntentConfirm(networkRule)
        paymentSheetPage.clickPrimaryButton()
    }

    private fun setupDeferredTest(testContext: ProductIntegrationTestRunnerContext) {
        challengeConfig.enqueueElementsSession(networkRule, isDeferredIntent = true)
        testContext.launch(isDeferredIntent = true)

        navigateToFormForLpm()

        challengeConfig.enqueuePaymentMethodCreate(networkRule)
        enqueueDeferredIntentRequests()

        createPaymentSheetPage().clickPrimaryButton()
    }

    private fun setupConfirmationTokenTest(testContext: PaymentSheetTestRunnerContext) {
        challengeConfig.enqueueElementsSession(networkRule, isDeferredIntent = true)
        presentPaymentSheetWithIntentConfiguration(testContext)

        navigateToFormForLpm()

        challengeConfig.enqueueConfirmationTokenCreate(networkRule)
        enqueueConfirmationTokenIntentRequests()

        createPaymentSheetPage().clickPrimaryButton()
    }

    private fun presentPaymentSheetWithIntentConfiguration(testContext: PaymentSheetTestRunnerContext) {
        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                )
            )
        }
    }

    private fun navigateToFormForLpm() {
        val verticalModePage = createPaymentSheetPage()
        verticalModePage.waitUntilVisible()
        verticalModePage.fillOutCardDetails()
    }

    private fun createPaymentSheetPage() = PaymentSheetPage(testRules.compose)

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

    private fun enqueueConfirmationTokenIntentRequests() {
        networkRule.enqueue(
            method("GET"),
            path(PAYMENT_INTENT_GET_PATH),
        ) { response ->
            response.testBodyFromFile(PAYMENT_INTENT_GET_FILE)
        }

        networkRule.enqueue(
            method("POST"),
            path(PAYMENT_INTENT_CONFIRM_PATH),
            bodyPart("confirmation_token", "ctoken_example"),
        ) { response ->
            response.testBodyFromFile(PAYMENT_INTENT_CONFIRM_FILE)
        }
    }

    /**
     * Represents different challenge configurations for testing.
     */
    internal sealed class ChallengeConfig(
        private val passiveCaptchaEnabled: Boolean,
        private val attestationEnabled: Boolean,
    ) {
        fun configureFeatureFlags(
            passiveCaptchaRule: FeatureFlagTestRule,
            attestationRule: FeatureFlagTestRule
        ) {
            passiveCaptchaRule.setEnabled(passiveCaptchaEnabled)
            attestationRule.setEnabled(attestationEnabled)
        }

        fun enqueueElementsSession(networkRule: NetworkRule, isDeferredIntent: Boolean) {
            networkRule.enqueue(
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile(
                    if (isDeferredIntent) DEFERRED_INTENT_ELEMENTS_SESSION_FILE else STANDARD_ELEMENTS_SESSION_FILE,
                    replacements = EMPTY_PAYMENT_METHODS_REPLACEMENTS + getElementsSessionReplacement()
                )
            }
        }

        abstract fun getElementsSessionReplacement(): ResponseReplacement

        fun enqueuePaymentIntentConfirm(networkRule: NetworkRule) {
            networkRule.enqueue(
                method("POST"),
                path(PAYMENT_INTENT_CONFIRM_PATH),
                hCaptchaTokenMatcher(NEW_PM_HCAPTCHA_TOKEN_PATH),
                attestationTokenMatcher(NEW_PM_ATTESTATION_TOKEN_PATH),
            ) { response ->
                response.testBodyFromFile(PAYMENT_INTENT_CONFIRM_FILE)
            }
        }

        fun enqueuePaymentMethodCreate(networkRule: NetworkRule) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_methods"),
                hCaptchaTokenMatcher(SAVED_PM_HCAPTCHA_TOKEN_PATH),
                attestationTokenMatcher(SAVED_PM_ATTESTATION_TOKEN_PATH),
            ) { response ->
                response.testBodyFromFile(PAYMENT_METHOD_CREATE_FILE)
            }
        }

        fun enqueueConfirmationTokenCreate(networkRule: NetworkRule) {
            // For new payment methods, radar_options are in payment_method_data
            networkRule.enqueue(
                method("POST"),
                path("/v1/confirmation_tokens"),
                hCaptchaTokenMatcher(NEW_PM_HCAPTCHA_TOKEN_PATH),
                attestationTokenMatcher(NEW_PM_ATTESTATION_TOKEN_PATH),
            ) { response ->
                response.testBodyFromFile(CONFIRMATION_TOKEN_CREATE_FILE)
            }
        }

        protected abstract fun hCaptchaTokenMatcher(path: String): RequestMatcher
        protected abstract fun attestationTokenMatcher(path: String): RequestMatcher

        class None : ChallengeConfig(
            passiveCaptchaEnabled = false,
            attestationEnabled = false,
        ) {
            override fun getElementsSessionReplacement() = ResponseReplacement(
                "\"unactivated_payment_method_types\": []",
                "\"unactivated_payment_method_types\": []"
            )

            override fun hCaptchaTokenMatcher(path: String): RequestMatcher =
                not(bodyPart(urlEncode(path), Regex(".+")))

            override fun attestationTokenMatcher(path: String): RequestMatcher =
                not(bodyPart(urlEncode(path), Regex(".+")))

            override fun toString() = "NoChallengesEnabled"
        }

        class BothEnabled : ChallengeConfig(
            passiveCaptchaEnabled = true,
            attestationEnabled = true,
        ) {
            override fun getElementsSessionReplacement() = ResponseReplacement(
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

            override fun hCaptchaTokenMatcher(path: String): RequestMatcher =
                bodyPart(urlEncode(path), HCAPTCHA_TOKEN)

            override fun attestationTokenMatcher(path: String): RequestMatcher =
                bodyPart(urlEncode(path), Regex(".+"))

            override fun toString() = "BothChallengesEnabled"
        }

        class OnlyPassiveCaptcha : ChallengeConfig(
            passiveCaptchaEnabled = true,
            attestationEnabled = false,
        ) {
            override fun getElementsSessionReplacement() = ResponseReplacement(
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

            override fun hCaptchaTokenMatcher(path: String): RequestMatcher =
                bodyPart(urlEncode(path), HCAPTCHA_TOKEN)

            override fun attestationTokenMatcher(path: String): RequestMatcher =
                not(bodyPart(urlEncode(path), Regex(".+")))

            override fun toString() = "OnlyPassiveCaptchaEnabled"
        }

        class OnlyAttestation : ChallengeConfig(
            passiveCaptchaEnabled = false,
            attestationEnabled = true,
        ) {
            override fun getElementsSessionReplacement() = ResponseReplacement(
                "\"unactivated_payment_method_types\": []",
                "\"unactivated_payment_method_types\": [],\n" +
                    "  \"flags\": {\n" +
                    "    \"elements_mobile_attest_on_intent_confirmation\": true\n" +
                    "  }"
            )

            override fun hCaptchaTokenMatcher(path: String): RequestMatcher =
                not(bodyPart(urlEncode(path), HCAPTCHA_TOKEN))

            override fun attestationTokenMatcher(path: String): RequestMatcher =
                bodyPart(urlEncode(path), Regex(".+"))

            override fun toString() = "OnlyAttestationEnabled"
        }
    }

    internal object ChallengeConfigProvider : TestParameterValuesProvider() {
        override fun provideValues(context: Context?): List<ChallengeConfig> {
            return listOf(
                ChallengeConfig.None(),
                ChallengeConfig.BothEnabled(),
                ChallengeConfig.OnlyPassiveCaptcha(),
                ChallengeConfig.OnlyAttestation(),
            )
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
        private const val CONFIRMATION_TOKEN_CREATE_FILE = "confirmation-token-create-with-new-card.json"

        private const val PAYMENT_INTENT_CONFIRM_PATH = "/v1/payment_intents/pi_example/confirm"
        private const val PAYMENT_INTENT_GET_PATH = "/v1/payment_intents/pi_example"

        private val EMPTY_PAYMENT_METHODS_REPLACEMENTS = listOf(
            ResponseReplacement("DEFAULT_PAYMENT_METHOD_HERE", "null"),
            ResponseReplacement("[PAYMENT_METHODS_HERE]", "[]")
        )
    }
}
