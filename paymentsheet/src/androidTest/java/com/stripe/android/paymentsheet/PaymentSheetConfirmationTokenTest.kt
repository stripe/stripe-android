package com.stripe.android.paymentsheet

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PaymentSheetConfirmationTokenTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    private val integrationType: IntegrationType = IntegrationType.Compose

    enum class CustomerType {
        NewCustomer,
//        ReturningCustomer,
    }
    enum class PaymentMethodType {
        Card,
        USBankAccount,
    }
    @Test
    fun testSuccessfulPayment(
        @TestParameter("false", "true") isLiveMode: Boolean,
        @TestParameter customerType: CustomerType,
        @TestParameter paymentMethodType: PaymentMethodType,
    ) = runPaymentSheetTest(
        networkRule = networkRule,
        isLiveMode = isLiveMode,
        builder = {
            createIntentCallback { _ ->
                CreateIntentResult.Success("pi_example_secret_example")
            }
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        verifyTestCombination(
            isLiveMode,
            customerType,
            paymentMethodType
        )
        presentSheet(testContext, customerType)
        completePaymentMethodSelection()
        confirm(isLiveMode)
    }

    private fun verifyTestCombination(
        isLiveMode: Boolean,
        customerType: CustomerType,
        paymentMethodType: PaymentMethodType,
    ) {
        Assume.assumeTrue(
            "Only need to verify client context is sent for test mode in any confirmation flow",
            !isLiveMode || (
                customerType == CustomerType.NewCustomer &&
                    paymentMethodType == PaymentMethodType.Card
                )
        )
    }

    private fun presentSheet(
        testContext: PaymentSheetTestRunnerContext,
        customerType: CustomerType
    ) {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }
        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration =  PaymentSheet.Configuration.Builder("Example, Inc.")
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .also {
//                        if (customerType == CustomerType.ReturningCustomer) {
//                            it.customer(
//                                PaymentSheet.CustomerConfiguration(
//                                    "cus_foobar",
//                                    "ek_test_foobar"
//                                )
//                            )
//                        }
                    }
                    .build()
            )
        }
    }

    private fun completePaymentMethodSelection(
    ) {
        page.fillOutCardDetails()
    }

    private fun confirm(
        isLiveMode: Boolean,
    ) {
        networkRule.enqueue(
            method("POST"),
            path("/v1/confirmation_tokens"),
            clientContext(isLiveMode),
            clientAttributionMetadataParamsForDeferredIntent(),
        ) { response ->
            response.testBodyFromFile("confirmation-token-create-with-new-card.json")
        }
        networkRule.enqueue(
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart("confirmation_token", "ctoken_example"),
            bodyPart("return_url", urlEncode("stripesdk://payment_return_url/com.stripe.android.paymentsheet.test")),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
        page.clickPrimaryButton()
    }

    private fun clientContext(isLiveMode: Boolean): RequestMatcher {
        // The client_context param is only sent in test mode when creating a confirmation token
        return if (isLiveMode) {
            not( bodyPart(urlEncode("client_context[mode]"), ".+".toRegex()))
        } else {
            // we only verify client context is not null here
            bodyPart(urlEncode("client_context[mode]"), "payment")
        }
    }
}