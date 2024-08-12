package com.stripe.android.paymentsheet

import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.IntegrationType
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
        integrationType = IntegrationType.Compose,
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

    private fun enqueueElementsSessionWithPaymentIntentAndCustomerSession() {
        enqueueElementsSession("elements-sessions-requires_pm_with_ps_pi_cs.json")
    }

    private fun enqueueElementsSessionWithSetupIntentAndCustomerSession() {
        enqueueElementsSession("elements-sessions-requires_pm_with_ps_si_cs.json")
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

    private fun enqueueElementsSession(responseFilePath: String) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(responseFilePath)
        }
    }

    private fun clickOnSaveForFutureUsage() {
        page.clickOnSaveForFutureUsage("Merchant, Inc.")
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
}
