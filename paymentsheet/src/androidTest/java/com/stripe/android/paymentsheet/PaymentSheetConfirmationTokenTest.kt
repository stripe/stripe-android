package com.stripe.android.paymentsheet

import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Assume
import org.junit.Rule
import org.junit.Test

internal class PaymentSheetConfirmationTokenTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(testRules.compose)

    enum class CustomerType {
        NewCustomer,
        ReturningCustomer,
    }

    enum class PaymentMethodType {
        Card,
        CashAppWithSetupFutureUsage,
        SavedCard,
        SavedCardWithCvcRecollection,
    }

    @Test
    fun testSuccessfulPayment_withClientContext() = testSuccessfulPayment(
        isLiveMode = false,
        customerType = CustomerType.NewCustomer,
        paymentMethodType = PaymentMethodType.Card,
    )

    @Test
    fun testSuccessfulPayment_withSavedCard() = testSuccessfulPayment(
        isLiveMode = true,
        customerType = CustomerType.ReturningCustomer,
        paymentMethodType = PaymentMethodType.SavedCard,
    )

    @Test
    fun testSuccessfulPayment_withCashAppAndSetupFutureUsage() = testSuccessfulPayment(
        isLiveMode = true,
        customerType = CustomerType.NewCustomer,
        paymentMethodType = PaymentMethodType.CashAppWithSetupFutureUsage,
    )

    @Test
    fun testSuccessfulPayment_withSavedCardAndCvcRecollection() = testSuccessfulPayment(
        isLiveMode = true,
        customerType = CustomerType.ReturningCustomer,
        paymentMethodType = PaymentMethodType.SavedCardWithCvcRecollection,
    )

    fun testSuccessfulPayment(
        isLiveMode: Boolean,
        customerType: CustomerType,
        paymentMethodType: PaymentMethodType,
    ) {
        verifyTestCombination(
            isLiveMode,
            customerType,
            paymentMethodType
        )
        runPaymentSheetTest(
            networkRule = networkRule,
            isLiveMode = isLiveMode,
            builder = {
                createIntentCallback { _ ->
                    CreateIntentResult.Success("pi_example_secret_example")
                }
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            presentSheet(testContext, customerType, paymentMethodType)
            completePaymentMethodSelection(customerType, paymentMethodType)
            confirm(isLiveMode, paymentMethodType)
        }
    }

    @Test
    fun testSuccessfulSetup() {
        runPaymentSheetTest(
            networkRule = networkRule,
            isLiveMode = false,
            builder = {
                createIntentCallback { _ ->
                    CreateIntentResult.Success("seti_example_secret_example")
                }
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            presentSheet(testContext, CustomerType.NewCustomer, PaymentMethodType.Card, isPayment = false)
            completePaymentMethodSelection(CustomerType.NewCustomer, PaymentMethodType.Card)
            confirm(false, PaymentMethodType.Card, isPayment = false)
        }
    }

    private fun verifyTestCombination(
        isLiveMode: Boolean,
        customerType: CustomerType,
        paymentMethodType: PaymentMethodType,
    ) {
        Assume.assumeTrue(
            "Only need to verify client context is sent for test mode in any confirmation flow",
            isLiveMode || (
                customerType == CustomerType.NewCustomer &&
                    paymentMethodType == PaymentMethodType.Card
                )
        )
        Assume.assumeFalse(
            "Only test saved cards with returning customers",
            customerType == CustomerType.NewCustomer && (
                paymentMethodType == PaymentMethodType.SavedCard ||
                    paymentMethodType == PaymentMethodType.SavedCardWithCvcRecollection
                )
        )
        Assume.assumeFalse(
            "Only test Cash App setup future usage with new customers",
            customerType == CustomerType.ReturningCustomer &&
                paymentMethodType == PaymentMethodType.CashAppWithSetupFutureUsage
        )
    }

    private fun presentSheet(
        testContext: PaymentSheetTestRunnerContext,
        customerType: CustomerType,
        paymentMethodType: PaymentMethodType,
        isPayment: Boolean = true,
    ) {
        if (paymentMethodType == PaymentMethodType.SavedCardWithCvcRecollection) {
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_cvc_recollection.json")
            }
        } else {
            networkRule.enqueue(
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
        }

        if (customerType == CustomerType.ReturningCustomer) {
            networkRule.enqueue(
                method("GET"),
                path("/v1/payment_methods"),
                query("type", "card"),
                query("customer", "cus_foobar"),
            ) { response ->
                response.testBodyFromFile("payment-methods-get-success.json")
            }
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = if (isPayment) {
                        PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 5099,
                            currency = "usd",
                            setupFutureUse = if (paymentMethodType == PaymentMethodType.CashAppWithSetupFutureUsage) {
                                PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
                            } else {
                                null
                            }
                        )
                    } else {
                        PaymentSheet.IntentConfiguration.Mode.Setup()
                    },
                    requireCvcRecollection = paymentMethodType == PaymentMethodType.SavedCardWithCvcRecollection
                ),
                configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .also {
                        if (customerType == CustomerType.ReturningCustomer) {
                            it.customer(
                                PaymentSheet.CustomerConfiguration(
                                    "cus_foobar",
                                    "ek_test_foobar"
                                )
                            )
                        }
                    }
                    .build()
            )
        }
    }

    private fun completePaymentMethodSelection(
        customerType: CustomerType,
        paymentMethodType: PaymentMethodType,
    ) {
        when (customerType) {
            CustomerType.NewCustomer -> {
                if (paymentMethodType == PaymentMethodType.CashAppWithSetupFutureUsage) {
                    page.clickOnLpm("cashapp")
                } else {
                    page.fillOutCardDetails()
                }
            }
            CustomerType.ReturningCustomer -> {
                when (paymentMethodType) {
                    PaymentMethodType.Card -> {
                        page.addPaymentMethod()
                        page.fillOutCardDetails()
                    }
                    PaymentMethodType.SavedCard -> {
                        page.clickSavedCard("4242")
                    }
                    PaymentMethodType.SavedCardWithCvcRecollection -> {
                        page.fillCvcRecollection("123")
                    }
                    else -> {
                        throw IllegalArgumentException("We do not test this payment method with returning customer")
                    }
                }
            }
        }
    }

    private fun confirm(
        isLiveMode: Boolean,
        paymentMethodType: PaymentMethodType,
        isPayment: Boolean = true,
    ) {
        networkRule.enqueue(
            method("POST"),
            path("/v1/confirmation_tokens"),
            clientContext(isLiveMode, isPayment),
            cvcRecollection(paymentMethodType),
            mandateDataAndSetupFutureUsage(paymentMethodType, isPayment),
            clientAttributionMetadataParamsForDeferredIntent(),
        ) { response ->
            if (isPayment) {
                response.testBodyFromFile("confirmation-token-create-with-new-card.json")
            } else {
                response.testBodyFromFile(
                    "confirmation-token-create-with-new-card.json",
                    listOf(
                        ResponseReplacement(
                            original = "\"payment_intent\": \"pi_example\"",
                            new = "\"setup_intent\": \"seti_example\"",
                        )
                    )
                )
            }
        }
        if (isPayment) {
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
        } else {
            networkRule.enqueue(
                path("/v1/setup_intents/seti_example"),
            ) { response ->
                response.testBodyFromFile("setup-intent-get.json")
            }
            networkRule.enqueue(
                method("POST"),
                path("/v1/setup_intents/seti_example/confirm"),
                bodyPart("confirmation_token", "ctoken_example"),
                bodyPart("return_url", urlEncode("stripesdk://payment_return_url/com.stripe.android.paymentsheet.test")),
            ) { response ->
                response.testBodyFromFile("setup-intent-confirm.json")
            }
        }
        page.clickPrimaryButton()
    }

    private fun clientContext(isLiveMode: Boolean, isPayment: Boolean = true): RequestMatcher {
        // The client_context param is only sent in test mode when creating a confirmation token
        return if (isLiveMode) {
            not(bodyPart(urlEncode("client_context[mode]"), ".+".toRegex()))
        } else {
            // we only verify client context is not null here
            bodyPart(
                urlEncode("client_context[mode]"),
                if (isPayment) {
                    "payment"
                } else {
                    "setup"
                }
            )
        }
    }

    private fun cvcRecollection(paymentMethodType: PaymentMethodType): RequestMatcher {
        return if (paymentMethodType == PaymentMethodType.SavedCardWithCvcRecollection) {
            bodyPart(
                urlEncode("payment_method_options[card][cvc]"),
                "123"
            )
        } else {
            not(
                bodyPart(
                    urlEncode("payment_method_options[card][cvc]"),
                    ".+".toRegex()
                )
            )
        }
    }

    private fun mandateDataAndSetupFutureUsage(
        paymentMethodType: PaymentMethodType,
        isPayment: Boolean = true
    ): RequestMatcher {
        return if (paymentMethodType == PaymentMethodType.CashAppWithSetupFutureUsage) {
            RequestMatchers.composite(
                bodyPart(
                    urlEncode("mandate_data[customer_acceptance][type]"),
                    "online"
                ),
                bodyPart(
                    urlEncode("setup_future_usage"),
                    urlEncode("off_session")
                ),
            )
        } else {
            RequestMatchers.composite(
                not(
                    bodyPart(
                        urlEncode("mandate_data[customer_acceptance][type]"),
                        ".+".toRegex()
                    )
                ),
                if (isPayment) {
                    not(
                        bodyPart(
                            urlEncode("setup_future_usage"),
                            ".+".toRegex()
                        )
                    )
                } else {
                    bodyPart(
                        urlEncode("setup_future_usage"),
                        urlEncode("off_session")
                    )
                }
            )
        }
    }
}
