package com.stripe.android.paymentsheet

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.IntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(CheckoutSessionPreview::class)
@RunWith(TestParameterInjector::class)
internal class PaymentSheetCheckoutSessionTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @TestParameter(valuesProvider = IntegrationTypeProvider::class)
    lateinit var integrationType: IntegrationType

    private val defaultConfiguration = PaymentSheet.Configuration(
        merchantDisplayName = "Checkout Session Test",
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    )

    /**
     * Test a successful card payment flow with checkout session.
     *
     * Flow:
     * 1. Present PaymentSheet with checkout session client secret
     * 2. Initialize checkout session (POST /v1/payment_pages/{cs_id}/init)
     * 3. Fill out card details
     * 4. Create payment method (POST /v1/payment_methods)
     * 5. Confirm checkout session (POST /v1/payment_pages/{cs_id}/confirm)
     * 6. Verify payment completed successfully
     */
    @Test
    fun testSuccessfulCardPaymentWithCheckoutSession() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        // Mock checkout session init API
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_pages/cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh/init"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        testContext.presentPaymentSheet {
            presentWithCheckoutSession(
                checkoutSessionClientSecret = "cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh_secret_example",
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        // Mock payment method creation
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        // Mock checkout session confirm API
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_pages/cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh/confirm"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        page.clickPrimaryButton()
    }
}
