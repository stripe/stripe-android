package com.stripe.android.paymentsheet

import androidx.test.espresso.intent.rule.IntentsRule
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.ConfirmationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

internal class HCaptchaTokenTest {
    private val testRules: TestRules = TestRules.create()
    private val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enablePassiveCaptcha,
        isEnabled = true
    )

    @get:Rule
    val rules: RuleChain = RuleChain.emptyRuleChain()
        .around(IntentsRule())
        .around(featureFlagTestRule)
        .around(testRules)

    private val networkRule = testRules.networkRule

    @Test
    fun newPaymentMethod_withPassiveCaptchaEnabled_includesHCaptchaTokenInConfirmRequest() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = ProductIntegrationType.PaymentSheet,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        setupNewPaymentMethodTest(testContext)
    }

    @Test
    fun savedPaymentMethod_withPassiveCaptchaEnabled_includesHCaptchaTokenInConfirmRequest() =
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupSavedPaymentMethodTest(testContext)
        }

    @Test
    fun paymentMethodCreation_withPassiveCaptchaEnabled_includesHCaptchaTokenInCreateRequest() =
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
            setupPaymentMethCreateWithDeferredTest(testContext)
        }

    private fun setupNewPaymentMethodTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithPassiveCaptcha(networkRule)
        testContext.launch()

        val paymentSheetPage = createPaymentSheetPage()
        paymentSheetPage.fillOutCardDetails()

        enqueuePaymentIntentConfirmWithHCaptcha(NEW_PM_HCAPTCHA_TOKEN_PATH)
        paymentSheetPage.clickPrimaryButton()
    }

    private fun setupSavedPaymentMethodTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithPassiveCaptchaForSavedCards(networkRule)
        enqueuePaymentMethodsGet()

        testContext.launch(configuration = createCustomerConfiguration())

        enqueuePaymentIntentConfirmWithHCaptcha(SAVED_PM_HCAPTCHA_TOKEN_PATH)

        val paymentSheetPage = createPaymentSheetPage()
        paymentSheetPage.fillCvcRecollection(CVC_VALUE)
        paymentSheetPage.clickPrimaryButton()
    }

    private fun setupPaymentMethCreateWithDeferredTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithPassiveCaptcha(networkRule, isDeferredIntent = true)
        testContext.launch(isDeferredIntent = true)

        navigateToFormForLpm()

        enqueuePaymentMethodCreateWithHCaptcha()
        enqueueDeferredIntentRequests()

        createPaymentSheetPage().clickPrimaryButton()
    }

    private fun navigateToFormForLpm() {
        val verticalModePage = createPaymentSheetPage()
        verticalModePage.waitUntilVisible()
        verticalModePage.fillOutCardDetails()
    }

    private fun createPaymentSheetPage() = PaymentSheetPage(testRules.compose)

    private fun createCustomerConfiguration() = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        customer = PaymentSheet.CustomerConfiguration(
            id = CUSTOMER_ID,
            ephemeralKeySecret = EPHEMERAL_KEY_SECRET,
        ),
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal
    )

    private fun enqueuePaymentMethodsGet() {
        networkRule.enqueue(
            host(API_HOST),
            method("GET"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile(PAYMENT_METHODS_GET_FILE)
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

    private fun enqueueElementsSessionWithPassiveCaptcha(
        networkRule: NetworkRule,
        isDeferredIntent: Boolean = false
    ) {
        enqueueElementsSessionWithCaptchaEnabled(
            networkRule = networkRule,
            baseFile = if (isDeferredIntent) {
                DEFERRED_INTENT_ELEMENTS_SESSION_FILE
            } else {
                STANDARD_ELEMENTS_SESSION_FILE
            },
            additionalReplacements = EMPTY_PAYMENT_METHODS_REPLACEMENTS
        )
    }

    private fun enqueueElementsSessionWithPassiveCaptchaForSavedCards(networkRule: NetworkRule) {
        enqueueElementsSessionWithCaptchaEnabled(
            networkRule = networkRule,
            baseFile = CVC_RECOLLECTION_ELEMENTS_SESSION_FILE,
            additionalReplacements = emptyList()
        )
    }

    private fun enqueueElementsSessionWithCaptchaEnabled(
        networkRule: NetworkRule,
        baseFile: String,
        additionalReplacements: List<ResponseReplacement>
    ) {
        val captchaReplacement = ResponseReplacement(
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
                baseFile,
                replacements = additionalReplacements + captchaReplacement
            )
        }
    }

    private fun enqueuePaymentIntentConfirmWithHCaptcha(tokenPath: String) {
        networkRule.enqueue(
            method("POST"),
            path(PAYMENT_INTENT_CONFIRM_PATH),
            bodyPart(urlEncode(tokenPath), HCAPTCHA_TOKEN),
        ) { response ->
            response.testBodyFromFile(PAYMENT_INTENT_CONFIRM_FILE)
        }
    }

    private fun enqueuePaymentMethodCreateWithHCaptcha() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(urlEncode(SAVED_PM_HCAPTCHA_TOKEN_PATH), HCAPTCHA_TOKEN),
        ) { response ->
            response.testBodyFromFile(PAYMENT_METHOD_CREATE_FILE)
        }
    }

    companion object {
        private const val HCAPTCHA_TOKEN = "20000000-aaaa-bbbb-cccc-000000000002"
        private const val HCAPTCHA_SITE_KEY = "20000000-ffff-ffff-ffff-000000000002"
        private const val NEW_PM_HCAPTCHA_TOKEN_PATH = "payment_method_data[radar_options][hcaptcha_token]"
        private const val SAVED_PM_HCAPTCHA_TOKEN_PATH = "radar_options[hcaptcha_token]"
        private const val CVC_VALUE = "123"
        private const val MERCHANT_DISPLAY_NAME = "Merchant, Inc."
        private const val CUSTOMER_ID = "cus_1"
        private const val EPHEMERAL_KEY_SECRET = "ek_123"
        private const val API_HOST = "api.stripe.com"

        private const val STANDARD_ELEMENTS_SESSION_FILE = "elements-sessions-with_pi_and_default_pms_enabled.json"
        private const val DEFERRED_INTENT_ELEMENTS_SESSION_FILE =
            "elements-sessions-deferred_intent_and_default_pms_enabled.json"
        private const val CVC_RECOLLECTION_ELEMENTS_SESSION_FILE = "elements-sessions-requires_cvc_recollection.json"
        private const val PAYMENT_INTENT_CONFIRM_FILE = "payment-intent-confirm.json"
        private const val PAYMENT_METHOD_CREATE_FILE = "payment-methods-create.json"
        private const val PAYMENT_METHODS_GET_FILE = "payment-methods-get-success.json"
        private const val PAYMENT_INTENT_GET_FILE = "payment-intent-get-requires_payment_method.json"

        private const val PAYMENT_INTENT_CONFIRM_PATH = "/v1/payment_intents/pi_example/confirm"
        private const val PAYMENT_INTENT_GET_PATH = "/v1/payment_intents/pi_example"

        private val EMPTY_PAYMENT_METHODS_REPLACEMENTS = listOf(
            ResponseReplacement("DEFAULT_PAYMENT_METHOD_HERE", "null"),
            ResponseReplacement("[PAYMENT_METHODS_HERE]", "[]")
        )
    }
}
