package com.stripe.android.paymentsheet

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.IntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
@RequiresIme
internal class PaymentSheetKeyboardTest {
    @get:Rule
    val testRules: TestRules = TestRules.create {
        around(IntentsRule())
    }

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule
    private val page = PaymentSheetPage(composeTestRule)

    @TestParameter(valuesProvider = IntegrationTypeProvider::class)
    lateinit var integrationType: IntegrationType

    @Test
    fun primaryButtonIsRevealedWhenCardFormBecomesComplete() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::expectNoResult,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        val paymentSheetActivity = testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Example, Inc.",
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                ),
            )
        }

        page.fillOutCardDetails(fillOutZipCode = false)
        page.assertPrimaryButtonEnabled(enabled = false)
        page.focusZipCode()
        composeTestRule.waitForKeyboardToBeVisible(paymentSheetActivity)
        page.assertPrimaryButtonBelowKeyboard(paymentSheetActivity)

        page.enterZipCode()
        page.assertPrimaryButtonEnabled(enabled = true)
        page.assertPrimaryButtonVisibleAboveKeyboard(paymentSheetActivity)

        testContext.markTestSucceeded()
    }
}
