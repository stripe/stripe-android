package com.stripe.android.paymentsheet

import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test

class PaymentSheetCustomerSessionTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @Test
    fun allowRedisplayIsUnspecifiedWhenNotSavingWithPaymentIntent() = runPaymentSheetTest(
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
            bodyPart(urlEncode("payment_method_data[allow_redisplay]"), allowRedisplay)
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }

    private fun enqueueSetupIntentConfirmWithExpectedAllowRedisplay(allowRedisplay: String) {
        return networkRule.enqueue(
            method("POST"),
            path("/v1/setup_intents/seti_example/confirm"),
            bodyPart(urlEncode("payment_method_data[allow_redisplay]"), allowRedisplay)
        ) { response ->
            response.testBodyFromFile("setup-intent-confirm.json")
        }
    }

    private fun enqueueElementsSession(
        responseFilePath: String,
        replacements: List<ResponseReplacement> = listOf()
    ) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(responseFilePath, replacements)
        }
    }

    private fun clickOnSaveForFutureUsage() {
        page.clickOnSaveForFutureUsage()
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
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

    @OptIn(ExperimentalCustomerSessionApi::class)
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
