package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountActivity
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class FormValidationTest {
    @get:Rule
    val testRules: TestRules = TestRules.create {
        around(IntentsRule())
    }

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @Test
    fun testCard(
        @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
        integrationType: ProductIntegrationType,
    ) = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::expectNoResult
    ) { testContext ->
        enqueueElementsSession()

        testContext.launch(configuration())

        navigateToFormFor(paymentMethodCode = "card")

        clickPrimaryButton()

        assertFieldErrorsAreShown()

        testContext.markTestSucceeded()
    }

    @Test
    fun testUsBankAccount(
        @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
        integrationType: ProductIntegrationType,
    ) = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::expectNoResult
    ) { testContext ->
        enqueueElementsSession()

        testContext.launch(configuration())

        navigateToFormFor(paymentMethodCode = "us_bank_account")

        clickPrimaryButton()

        assertDoesNotLaunchBankAccountFlow()
        assertFieldErrorsAreShown()

        testContext.markTestSucceeded()
    }

    private fun configuration() = PaymentSheet.Configuration.Builder("Example, Inc.")
        .billingDetailsCollectionConfiguration(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            )
        )
        .allowsDelayedPaymentMethods(true)
        .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
        .build()

    private fun enqueueElementsSession() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(
                filename = "elements-sessions-with_pi_and_default_pms_enabled.json",
                replacements = listOf(
                    ResponseReplacement(
                        original = "DEFAULT_PAYMENT_METHOD_HERE",
                        new = "null"
                    ),
                    ResponseReplacement(
                        original = "[PAYMENT_METHODS_HERE]",
                        new = "[]",
                    )
                )
            )
        }
    }

    private fun navigateToFormFor(
        paymentMethodCode: String
    ) = page.clickOnLpm(paymentMethodCode, forVerticalMode = true)

    private fun assertDoesNotLaunchBankAccountFlow() {
        intended(
            hasComponent(
                CollectBankAccountActivity::class.java.name
            ),
            times(0)
        )
    }

    private fun clickPrimaryButton() {
        composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onAllNodes(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG).and(isNotEnabled()))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onAllNodes(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG))
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
    }

    private fun assertFieldErrorsAreShown() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasText("This field cannot be blank."))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
