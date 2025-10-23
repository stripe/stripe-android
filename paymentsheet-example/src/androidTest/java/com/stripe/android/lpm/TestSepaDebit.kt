package com.stripe.android.lpm

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.GooglePaySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodOptionsSetupFutureUsageOverrideSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.TermsDisplay
import com.stripe.android.paymentsheet.example.playground.settings.TermsDisplaySettingsDefinition
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT
import com.stripe.android.test.core.TestParameters
import com.stripe.android.test.core.ui.ComposeButton
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestSepaDebit : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "sepa_debit",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.FR
        settings[DelayedPaymentMethodsSettingsDefinition] = true
        settings[GooglePaySettingsDefinition] = false
        settings[InitializationTypeSettingsDefinition] = InitializationType.DeferredClientSideConfirmation
    }.copy(
        authorizationAction = null,
    )

    @Test
    fun testSepaDebit() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copy(
                saveForFutureUseCheckboxVisible = true,
            ),
        ) {
            fillOutIban()
        }
    }

    @Test
    fun testSepaDebitSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[AutomaticPaymentMethodsSettingsDefinition] = true
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
                settings[TermsDisplaySettingsDefinition] = TermsDisplay.NEVER_SEPA_FAMILY
            },
        ) {
            fillOutIban()
        }
    }

    @Test
    fun testSepaDebitSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[AutomaticPaymentMethodsSettingsDefinition] = true
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            },
        ) {
            fillOutIban()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSavedWithPaymentIntentAndCheckbox() {
        val testParameters = testParameters.copy(
            saveForFutureUseCheckboxVisible = true,
            saveCheckboxValue = true,
        )

        val state = testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            populateCustomLpmFields = {
                fillOutIban()
            },
        )

        testDriver.confirmCompleteWithDefaultSavedPaymentMethod(
            customerId = state?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters,
            beforeBuyAction = { selectors ->
                selectors.composeTestRule.waitUntilExactlyOneExists(
                    matcher = hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
                        .and(isSelected())
                        .and(hasText(IBAN.takeLast(4), substring = true)),
                    timeoutMillis = 5000L
                )
            },
        )
    }

    @Test
    fun testSepaDebitDefaultReturningUserFlowWithoutShowingFlowController() {
        val testParameters = testParameters.copyPlaygroundSettings { settings ->
            settings[AutomaticPaymentMethodsSettingsDefinition] = true
            settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
        }

        // Create the payment method and set it as default by going through the whole flow.
        val playgroundState = testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        ) {
            fillOutIban()
        }

        testDriver.confirmCustomWithDefaultSavedPaymentMethod(
            customerId = playgroundState?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters,
            afterBuyAction = {
                ComposeButton(rules.compose, hasTestTag("SEPA_MANDATE_CONTINUE_BUTTON"))
                    .waitForEnabled()
                    .click()
            }
        )
    }

    @Test
    fun testSepaDebitDefaultReturningUserFlowWithShowingFlowController() {
        val testParameters = testParameters.copyPlaygroundSettings { settings ->
            settings[AutomaticPaymentMethodsSettingsDefinition] = true
            settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
        }

        // Create the payment method and set it as default by going through the whole flow.
        val playgroundState = testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        ) {
            fillOutIban()
        }

        testDriver.confirmCustomWithDefaultSavedPaymentMethod(
            customerId = playgroundState?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters,
            beforeBuyAction = { selectors ->
                selectors.multiStepSelect.click()
                selectors.paymentSelection.click()
                selectors.continueButton.click()
            }
        )
    }

    @Test
    fun testSepaDebitWithPmoSfuAndCustomerSession() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSessionSettingsDefinition] = true
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[PaymentMethodOptionsSetupFutureUsageOverrideSettingsDefinition] = "sepa_debit:off_session"
            }.copy(saveForFutureUseCheckboxVisible = true),
        ) {
            fillOutIban()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun fillOutIban() {
        rules.compose.waitUntilAtLeastOneExists(
            hasText("IBAN"),
            timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
        )
        rules.compose.onNodeWithText("IBAN").apply {
            performTextInput(IBAN)
        }
    }

    private companion object {
        const val IBAN = "DE89370400440532013000"
    }
}
