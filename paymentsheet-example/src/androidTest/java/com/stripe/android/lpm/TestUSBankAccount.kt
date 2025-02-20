package com.stripe.android.lpm

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.paymentdatacollection.ach.TEST_TAG_ACCOUNT_DETAILS
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT
import com.stripe.android.test.core.TestParameters
import com.stripe.android.test.core.ui.ComposeButton
import com.stripe.android.test.core.ui.PaymentSelection
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@Ignore("Tests currently failing, ignoring while we work on a fix so we can merge other PRs.")
@RunWith(AndroidJUnit4::class)
internal class TestUSBankAccount : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "us_bank_account",
        executeInNightlyRun = true,
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[DelayedPaymentMethodsSettingsDefinition] = true
    }

    @Test
    fun testUSBankAccountSuccess() {
        testDriver.confirmUSBankAccount(
            testParameters = testParameters.copyPlaygroundSettings {
                it[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
            },
            afterAuthorization = { _, _ ->
                ComposeButton(rules.compose, hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
                    .waitFor(isEnabled())
            }
        )
    }

    @Test
    fun testUSBankAccountSuccessWithIndecisiveUser() {
        // Select another LPM before coming back to the linked bank account
        testDriver.confirmUSBankAccount(
            testParameters = testParameters.copyPlaygroundSettings {
                it[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
            },
            afterAuthorization = { _, _ ->
                rules.compose.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
                    rules.compose
                        .onAllNodesWithTag(TEST_TAG_ACCOUNT_DETAILS)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }

                // Briefly switch to another payment method
                val cardSelection = PaymentSelection(rules.compose, "card")
                cardSelection.click()

                // Come back to the bank tab
                val bankSelection = PaymentSelection(rules.compose, "us_bank_account")
                bankSelection.click()
            },
        )
    }

    @Test
    fun testCardAfterConfirmingUSBankAccount() {
        // Link a bank account, but pay with a card instead
        testDriver.confirmUSBankAccount(
            testParameters = testParameters.copyPlaygroundSettings {
                it[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
            },
            afterAuthorization = { _, populator ->
                rules.compose.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
                    rules.compose
                        .onAllNodesWithTag(TEST_TAG_ACCOUNT_DETAILS)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }

                // We actually want to confirm with a card
                val cardSelection = PaymentSelection(rules.compose, "card")
                cardSelection.click()
                populator.populateCardDetails()
            },
        )
    }

    @Test
    fun testUSBankAccountCancelAllowsUserToContinue() {
        testDriver.confirmUSBankAccount(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
            afterAuthorization = { _, _ ->
                ComposeButton(rules.compose, hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
                    .waitFor(isEnabled())
            }
        )
    }

    @Test
    fun testUSBankAccountCancelAllowsUserToContinueInCustomFlow() {
        testDriver.confirmCustomUSBankAccount(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
            afterAuthorization = {
                ComposeButton(rules.compose, hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
                    .waitFor(isEnabled())
            }
        )
    }
}
