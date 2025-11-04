package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import com.stripe.android.attestation.AttestationActivityContract
import com.stripe.android.attestation.AttestationActivityResult
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
import com.stripe.android.testing.RetryRule
import com.stripe.android.testing.ShampooRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

internal class ConfirmWithAttestationTest {
    private val testRules: TestRules = TestRules.create()
    private val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableAttestationOnIntentConfirmation,
        isEnabled = true
    )

    @get:Rule
    val rules: RuleChain = RuleChain.emptyRuleChain()
        .around(IntentsRule())
        .around(featureFlagTestRule)
        .around(testRules)
        .around(RetryRule(3))

    private val networkRule = testRules.networkRule

    @Test
    fun newPaymentMethod_withAttestationEnabled_includesAndroidVerificationObjectInConfirmRequest() =
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupNewPaymentMethodTest(testContext)
        }

    @Test
    fun savedPaymentMethod_withAttestationEnabled_includesAndroidVerificationObjectInConfirmRequest() =
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = ProductIntegrationType.PaymentSheet,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            setupSavedPaymentMethodTest(testContext)
        }

    @Test
    fun paymentMethodCreation_withAttestationEnabled_includesAndroidVerificationObjectInCreateRequest() =
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
            setupPaymentMethodCreateWithDeferredTest(testContext)
        }

    private fun setupNewPaymentMethodTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithAttestation(networkRule)

        intendedActivityResult(AttestationActivityResult.Success(ATTESTATION_TOKEN))

        testContext.launch()

        val paymentSheetPage = createPaymentSheetPage()
        paymentSheetPage.fillOutCardDetails()

        enqueuePaymentIntentConfirmWithAttestation(NEW_PM_ATTESTATION_TOKEN_PATH)
        paymentSheetPage.clickPrimaryButton()
    }

    private fun setupSavedPaymentMethodTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithAttestationForSavedCards(networkRule)
        enqueuePaymentMethodsGet()

        intendedActivityResult(AttestationActivityResult.Success(ATTESTATION_TOKEN))

        testContext.launch(configuration = createCustomerConfiguration())

        enqueuePaymentIntentConfirmWithAttestation(SAVED_PM_ATTESTATION_TOKEN_PATH)

        val paymentSheetPage = createPaymentSheetPage()
        paymentSheetPage.fillCvcRecollection(CVC_VALUE)
        paymentSheetPage.clickPrimaryButton()
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

    private fun setupPaymentMethodCreateWithDeferredTest(testContext: ProductIntegrationTestRunnerContext) {
        enqueueElementsSessionWithAttestation(networkRule, isDeferredIntent = true)

        intendedActivityResult(AttestationActivityResult.Success(ATTESTATION_TOKEN))

        testContext.launch(isDeferredIntent = true)

        navigateToFormForLpm()

        enqueuePaymentMethodCreateWithAttestation()
        enqueueDeferredIntentRequests()

        createPaymentSheetPage().clickPrimaryButton()
    }

    private fun navigateToFormForLpm() {
        val verticalModePage = createPaymentSheetPage()
        verticalModePage.waitUntilVisible()
        verticalModePage.fillOutCardDetails()
    }

    private fun enqueueElementsSessionWithAttestation(
        networkRule: NetworkRule,
        isDeferredIntent: Boolean = false
    ) {
        enqueueElementsSessionWithAttestationEnabled(
            networkRule = networkRule,
            baseFile = if (isDeferredIntent) {
                DEFERRED_INTENT_ELEMENTS_SESSION_FILE
            } else {
                STANDARD_ELEMENTS_SESSION_FILE
            },
            additionalReplacements = EMPTY_PAYMENT_METHODS_REPLACEMENTS
        )
    }

    private fun enqueueElementsSessionWithAttestationForSavedCards(networkRule: NetworkRule) {
        enqueueElementsSessionWithAttestationEnabled(
            networkRule = networkRule,
            baseFile = CVC_RECOLLECTION_ELEMENTS_SESSION_FILE,
            additionalReplacements = emptyList()
        )
    }

    private fun enqueueElementsSessionWithAttestationEnabled(
        networkRule: NetworkRule,
        baseFile: String,
        additionalReplacements: List<ResponseReplacement>
    ) {
        val attestationReplacement = ResponseReplacement(
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
                baseFile,
                replacements = additionalReplacements + attestationReplacement
            )
        }
    }

    private fun enqueuePaymentIntentConfirmWithAttestation(tokenPath: String) {
        networkRule.enqueue(
            method("POST"),
            path(PAYMENT_INTENT_CONFIRM_PATH),
            bodyPart(urlEncode(tokenPath), ATTESTATION_TOKEN),
        ) { response ->
            response.testBodyFromFile(PAYMENT_INTENT_CONFIRM_FILE)
        }
    }

    private fun enqueuePaymentMethodCreateWithAttestation() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(urlEncode(SAVED_PM_ATTESTATION_TOKEN_PATH), ATTESTATION_TOKEN),
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

    private fun intendedActivityResult(result: AttestationActivityResult) {
        intending(hasComponent(ATTESTATION_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra(AttestationActivityContract.EXTRA_RESULT, result)
            )
        )
    }

    companion object {
        private const val ATTESTATION_ACTIVITY_NAME = "com.stripe.android.attestation.AttestationActivity"
        private const val ATTESTATION_TOKEN = "attestation_token_123"
        private const val NEW_PM_ATTESTATION_TOKEN_PATH =
            "payment_method_data[radar_options][android_verification_object][android_verification_token]"
        private const val SAVED_PM_ATTESTATION_TOKEN_PATH =
            "radar_options[android_verification_object][android_verification_token]"
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
