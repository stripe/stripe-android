package com.stripe.android.paymentsheet

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountActivity
import com.stripe.android.paymentsheet.utils.TestRules
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

    @Test
    fun testCard(
        @TestParameter(valuesProvider = FormValidationIntegrationType.Provider::class)
        integrationType: FormValidationIntegrationType,
    ) = runFormValidationIntegrationTest(
        networkRule = networkRule,
        composeTestRule = composeTestRule,
        integrationType = integrationType,
    ) {
        enqueueElementsSession()

        launch()

        navigateToFormFor(paymentMethodCode = "card")

        clickDisabledPrimaryButton()

        assertFieldErrorsAreShown()

        markTestSucceeded()
    }

    @Test
    fun testUsBankAccount(
        @TestParameter(valuesProvider = FormValidationIntegrationType.Provider::class)
        integrationType: FormValidationIntegrationType,
    ) = runFormValidationIntegrationTest(
        networkRule = networkRule,
        composeTestRule = composeTestRule,
        integrationType = integrationType,
    ) {
        enqueueElementsSession()

        launch()

        navigateToFormFor(paymentMethodCode = "us_bank_account")

        clickDisabledPrimaryButton()

        assertDoesNotLaunchBankAccountFlow()
        assertFieldErrorsAreShown()

        markTestSucceeded()
    }

    private fun enqueueElementsSession() {
        networkRule.elementsSession { response ->
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

    private fun assertDoesNotLaunchBankAccountFlow() {
        intended(
            hasComponent(
                CollectBankAccountActivity::class.java.name
            ),
            times(0)
        )
    }

    private fun assertFieldErrorsAreShown() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(
                    hasText(BLANK_FIELD_ERROR).or(
                        SemanticsMatcher.expectValue(SemanticsProperties.Error, BLANK_FIELD_ERROR)
                    )
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        const val BLANK_FIELD_ERROR = "This field cannot be blank."
    }
}
