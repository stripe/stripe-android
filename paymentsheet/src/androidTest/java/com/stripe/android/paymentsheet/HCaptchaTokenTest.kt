package com.stripe.android.paymentsheet

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.intent.rule.IntentsRule
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.ConfirmationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import kotlin.time.Duration.Companion.seconds

internal class HCaptchaTokenTest {
    // The /v1/consumers/sessions/log_out request is launched async from a GlobalScope. We want to make sure it happens,
    // but it's okay if it takes a bit to happen.
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)
    private val testRules: TestRules = TestRules.create(networkRule = networkRule)

    @get:Rule
    val rules: RuleChain = RuleChain.emptyRuleChain()
        .around(IntentsRule())
        .around(testRules)

    @Test
    fun newPaymentMethod_withPassiveCaptchaEnabled_includesHCaptchaTokenInConfirmRequest() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = ProductIntegrationType.PaymentSheet,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        setupNewPaymentMethodTest(testContext)
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

    @Test
    fun linkPaymentMethodMode_withPassiveCaptchaEnabled_includesHCaptchaTokenInConfirmRequest() =
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupLinkWithCaptchaTest(
                testContext = testContext,
                elementsSessionFile = LINK_PMM_ELEMENTS_SESSION_FILE,
            ) {
                enqueueConsumerPaymentDetails()

                enqueuePaymentIntentConfirmWithHCaptcha(NEW_PM_HCAPTCHA_TOKEN_PATH)
            }
        }

    @Test
    fun linkPassthroughMode_withPassiveCaptchaEnabled_includesHCaptchaTokenInConfirmRequest() =
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupLinkWithCaptchaTest(
                testContext = testContext,
                elementsSessionFile = LINK_PASSTHROUGH_ELEMENTS_SESSION_FILE,
            ) {
                enqueueConsumerPaymentDetails()

                enqueueConsumerPaymentDetailsShare()

                enqueuePaymentIntentConfirmWithHCaptcha(SAVED_PM_HCAPTCHA_TOKEN_PATH)
            }
        }

    private fun setupNewPaymentMethodTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithPassiveCaptcha(networkRule)
        testContext.launch()

        val paymentSheetPage = createPaymentSheetPage()
        paymentSheetPage.fillOutCardDetails()

        enqueuePaymentIntentConfirmWithHCaptcha(NEW_PM_HCAPTCHA_TOKEN_PATH)
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

    private fun setupLinkWithCaptchaTest(
        testContext: ProductIntegrationTestRunnerContext,
        elementsSessionFile: String,
        enqueuePaymentDetailsAndConfirm: () -> Unit,
    ) {
        enqueueElementsSessionWithLinkAndCaptcha(elementsSessionFile)
        testContext.launch()

        val page = createPaymentSheetPage()
        page.fillOutCardDetails()

        enqueueConsumerSessionLookup()

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()

        closeSoftKeyboard()

        enqueueConsumerSignUp()

        enqueuePaymentDetailsAndConfirm()

        enqueueConsumerLogout()

        page.clickPrimaryButton()
    }

    private fun createPaymentSheetPage() = PaymentSheetPage(testRules.compose)

    private fun enqueueConsumerSessionLookup() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }
    }

    private fun enqueueConsumerSignUp() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }
    }

    private fun enqueueConsumerPaymentDetails() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details")
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }
    }

    private fun enqueueConsumerPaymentDetailsShare() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details/share"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-share-success.json")
        }
    }

    private fun enqueueConsumerLogout() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/log_out"),
        ) { response ->
            response.testBodyFromFile("consumer-session-logout-success.json")
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

    private fun enqueueElementsSessionWithLinkAndCaptcha(file: String) {
        enqueueElementsSessionWithCaptchaEnabled(
            networkRule = networkRule,
            baseFile = file,
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

        networkRule.elementsSession { response ->
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
            bodyPart(urlEncode(tokenPath), HCAPTCHA_TOKEN)
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

        private const val LINK_PMM_ELEMENTS_SESSION_FILE = "elements-sessions-requires_payment_method.json"
        private const val LINK_PASSTHROUGH_ELEMENTS_SESSION_FILE =
            "elements-sessions-requires_pm_with_link_ps_mode.json"
        private const val STANDARD_ELEMENTS_SESSION_FILE = "elements-sessions-with_pi_and_default_pms_enabled.json"
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
