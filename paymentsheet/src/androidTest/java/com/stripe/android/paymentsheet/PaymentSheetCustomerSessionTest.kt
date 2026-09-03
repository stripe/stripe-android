package com.stripe.android.paymentsheet

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.paymentsheet.utils.ApiConfigurationTestType
import com.stripe.android.paymentsheet.utils.ApiConfigurationTestTypeProvider
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class PaymentSheetCustomerSessionTest(
    @TestParameter(valuesProvider = ApiConfigurationTestTypeProvider::class)
    private val apiConfigurationTestType: ApiConfigurationTestType,
) {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @Test
    fun allowRedisplayIsUnspecifiedWhenNotSavingWithPaymentIntent() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        enqueueElementsSessionWithPaymentIntentAndCustomerSession()

        testContext.presentWithPaymentIntent()

        page.fillOutCardDetails()

        enqueuePaymentIntentConfirmWithExpectedAllowRedisplay(allowRedisplay = "unspecified")

        page.clickPrimaryButton()
    }

    @Test
    fun allowRedisplayIsAlwaysWhenSavingWithPaymentIntent() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        enqueueElementsSessionWithPaymentIntentAndCustomerSession()

        testContext.presentWithPaymentIntent()

        page.fillOutCardDetails()
        clickOnSaveForFutureUsage()

        enqueuePaymentIntentConfirmWithExpectedAllowRedisplay(allowRedisplay = "always")

        page.clickPrimaryButton()
    }

    @Test
    fun allowRedisplayIsLimitedWhenNotSavingWithSetupIntent() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        enqueueElementsSessionWithSetupIntentAndCustomerSession()

        testContext.presentWithSetupIntent()

        page.fillOutCardDetails()

        enqueueSetupIntentConfirmWithExpectedAllowRedisplay(allowRedisplay = "limited")

        page.clickPrimaryButton()
    }

    @Test
    fun allowRedisplayIsAlwaysWhenSavingWithSetupIntent() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        enqueueElementsSessionWithSetupIntentAndCustomerSession()

        testContext.presentWithSetupIntent()
        clickOnSaveForFutureUsage()

        page.fillOutCardDetails()

        enqueueSetupIntentConfirmWithExpectedAllowRedisplay(allowRedisplay = "always")

        page.clickPrimaryButton()
    }

    @Test
    fun allowRedisplayIsUnspecifiedWhenSaveIsDisabledWithPaymentIntent() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        enqueueElementsSessionWithPaymentIntentAndCustomerSession(
            isSaveEnabled = false,
        )

        testContext.presentWithPaymentIntent()

        page.fillOutCardDetails()

        enqueuePaymentIntentConfirmWithExpectedAllowRedisplay(allowRedisplay = "unspecified")

        page.clickPrimaryButton()
    }

    @Test
    fun allowRedisplayIsLimitedWhenSaveIsDisabledWithSetupIntent() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        enqueueElementsSessionWithSetupIntentAndCustomerSession(
            isSaveEnabled = false,
        )

        testContext.presentWithSetupIntent()

        page.fillOutCardDetails()

        enqueueSetupIntentConfirmWithExpectedAllowRedisplay(allowRedisplay = "limited")

        page.clickPrimaryButton()
    }

    @Test
    fun allowRedisplayIsUnspecifiedWhenOverrideIsUnspecifiedWithSetupIntent() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        enqueueElementsSessionWithSetupIntentAndCustomerSession(
            isSaveEnabled = false,
            allowRedisplayOverride = "unspecified",
        )

        testContext.presentWithSetupIntent()

        page.fillOutCardDetails()

        enqueueSetupIntentConfirmWithExpectedAllowRedisplay(allowRedisplay = "unspecified")

        page.clickPrimaryButton()
    }

    @Test
    fun allowRedisplayIsAlwaysWhenOverrideIsAlwaysWithSetupIntent() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        enqueueElementsSessionWithSetupIntentAndCustomerSession(
            isSaveEnabled = false,
            allowRedisplayOverride = "always",
        )

        testContext.presentWithSetupIntent()

        page.fillOutCardDetails()

        enqueueSetupIntentConfirmWithExpectedAllowRedisplay(allowRedisplay = "always")

        page.clickPrimaryButton()
    }

    private fun enqueueElementsSessionWithPaymentIntentAndCustomerSession(
        isSaveEnabled: Boolean = true,
        allowRedisplayOverride: String? = null,
    ) {
        enqueueElementsSession(
            responseFilePath = "elements-sessions-requires_pm_with_ps_pi_cs.json",
            replacements = createReplacements(isSaveEnabled, allowRedisplayOverride),
        )
    }

    private fun enqueueElementsSessionWithSetupIntentAndCustomerSession(
        isSaveEnabled: Boolean = true,
        allowRedisplayOverride: String? = null,
    ) {
        enqueueElementsSession(
            responseFilePath = "elements-sessions-requires_pm_with_ps_si_cs.json",
            replacements = createReplacements(isSaveEnabled, allowRedisplayOverride),
        )
    }

    private fun enqueuePaymentIntentConfirmWithExpectedAllowRedisplay(allowRedisplay: String) {
        return networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart("payment_method_data[allow_redisplay]", allowRedisplay)
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }

    private fun enqueueSetupIntentConfirmWithExpectedAllowRedisplay(allowRedisplay: String) {
        return networkRule.enqueue(
            method("POST"),
            path("/v1/setup_intents/seti_example/confirm"),
            bodyPart("payment_method_data[allow_redisplay]", allowRedisplay)
        ) { response ->
            response.testBodyFromFile("setup-intent-confirm.json")
        }
    }

    private fun enqueueElementsSession(
        responseFilePath: String,
        replacements: List<ResponseReplacement> = listOf()
    ) {
        networkRule.elementsSession { response ->
            response.testBodyFromFile(responseFilePath, replacements)
        }
    }

    private fun clickOnSaveForFutureUsage() {
        page.clickOnSaveForFutureUsage()
    }

    private fun PaymentSheetTestRunnerContext.presentWithPaymentIntent() {
        presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_1",
                    ),
                ),
            )
        }
    }

    private fun PaymentSheetTestRunnerContext.presentWithSetupIntent() {
        presentPaymentSheet {
            presentWithSetupIntent(
                setupIntentClientSecret = "seti_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_1",
                    ),
                ),
            )
        }
    }

    private fun createReplacements(
        isSaveEnabled: Boolean,
        allowRedisplayOverride: String?
    ): List<ResponseReplacement> {
        val replacements = mutableListOf<ResponseReplacement>()

        if (!isSaveEnabled) {
            replacements.add(
                ResponseReplacement(
                    original = """
                        "payment_method_save": "enabled"
                    """.trimIndent(),
                    new = """
                        "payment_method_save": "disabled"
                    """.trimIndent(),
                )
            )
        }

        allowRedisplayOverride?.let {
            replacements.add(
                ResponseReplacement(
                    original = """
                        "payment_method_save_allow_redisplay_override": null
                    """.trimIndent(),
                    new = """
                        "payment_method_save_allow_redisplay_override": "$it"
                    """.trimIndent(),
                )
            )
        }

        return replacements
    }
}
