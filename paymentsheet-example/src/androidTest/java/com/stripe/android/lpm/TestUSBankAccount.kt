package com.stripe.android.lpm

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestUSBankAccount : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "us_bank_account",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[DelayedPaymentMethodsSettingsDefinition] = true
    }

    @Test
    fun testUSBankAccountCancelAllowsUserToContinue() {
        testDriver.confirmUSBankAccount(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
            afterAuthorization = {
                rules.compose.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
                    .assertIsEnabled()
            }
        )
    }

    @Test
    fun testUSBankAccountInCustomFlow() {
        testDriver.confirmCustomUSBankAccount(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
            afterAuthorization = {
                rules.compose.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
                    .assertIsEnabled()
            }
        )
    }
}
