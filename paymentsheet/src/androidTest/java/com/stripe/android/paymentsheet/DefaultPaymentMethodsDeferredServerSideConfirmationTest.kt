package com.stripe.android.paymentsheet

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.DefaultPaymentMethodsUtils
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutType
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class DefaultPaymentMethodsDeferredServerSideConfirmationTest {

    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
    lateinit var integrationType: ProductIntegrationType

    // Confirmation behavior between horizontal and vertical doesn't differ, so we're testing with vertical mode only.
    private val layoutType = PaymentSheetLayoutType.Vertical()

    @Test
    fun setNewCardAsDefault_withSavedPaymentMethods_failsInTestMode() = runProductIntegrationTest(
        networkRule = networkRule,
        builder = {
            createIntentCallback { _, _ ->
                CreateIntentResult.Success(clientSecret = "pi_example_secret_example")
            }
        },
        integrationType = integrationType,
        resultCallback = integrationType.expectedDeferredSSCResultCallback,
    ) { testContext ->
        val paymentSheetPage = PaymentSheetPage(composeTestRule)

        val cards = listOf(
            PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
            PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
        )

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
            networkRule = networkRule,
            cards = cards,
            isDeferredIntent = true,
        )

        DefaultPaymentMethodsUtils.launch(
            testContext = testContext,
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = true,
            isDeferredIntent = true,
        )

        layoutType.openNewCardForm(
            composeTestRule = composeTestRule,
        )

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.checkSetAsDefaultCheckbox()

        paymentSheetPage.assertSetAsDefaultCheckboxChecked()

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-success.json")
        }

        paymentSheetPage.clickPrimaryButton()

        integrationType.assertDefaultPaymentMethodsDeferredSSCErrorShown(
            composeTestRule = composeTestRule,
            testContext = testContext,
        )
    }

    @Test
    fun addFirstCardForUser_failsInTestMode() = runProductIntegrationTest(
        networkRule = networkRule,
        builder = {
            createIntentCallback { _, _ ->
                CreateIntentResult.Success(clientSecret = "pi_example_secret_example")
            }
        },
        integrationType = integrationType,
        resultCallback = integrationType.expectedDeferredSSCResultCallback,
    ) { testContext ->
        val paymentSheetPage = PaymentSheetPage(composeTestRule)

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
            networkRule = networkRule,
            isDeferredIntent = true,
        )

        DefaultPaymentMethodsUtils.launch(
            testContext = testContext,
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = false,
            isDeferredIntent = true,
        )

        layoutType.openNewCardForm(
            composeTestRule = composeTestRule,
        )

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-success.json")
        }

        paymentSheetPage.clickPrimaryButton()

        integrationType.assertDefaultPaymentMethodsDeferredSSCErrorShown(
            composeTestRule = composeTestRule,
            testContext = testContext,
        )
    }
}
