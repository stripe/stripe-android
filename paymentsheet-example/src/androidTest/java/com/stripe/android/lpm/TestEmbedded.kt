package com.stripe.android.lpm

import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test

internal class TestEmbedded : BasePlaygroundTest() {
    @Test
    fun testCard() {
        testDriver.confirmEmbedded(
            testParameters = TestParameters.create(
                paymentMethodCode = "card",
                authorizationAction = null,
                executeInNightlyRun = true,
            ).copy(
                saveForFutureUseCheckboxVisible = true,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCashAppPay() {
        testDriver.confirmEmbedded(
            testParameters = TestParameters.create(
                paymentMethodCode = "cashapp",
                executeInNightlyRun = true,
            ) { settings ->
                settings[CountrySettingsDefinition] = Country.US
                settings[CurrencySettingsDefinition] = Currency.USD
                settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
                    PaymentMethod.Type.Card,
                    PaymentMethod.Type.CashAppPay
                ).joinToString(",")
            },
            values = null, // We don't show the form for cash app.
        )
    }

    @Test
    fun testUsBankAccount() {
        testDriver.confirmEmbeddedUsBankAccount(
            testParameters = TestParameters.create(
                paymentMethodCode = "us_bank_account",
                authorizationAction = null,
                executeInNightlyRun = true,
            ){ settings ->
                settings[CountrySettingsDefinition] = Country.US
                settings[CurrencySettingsDefinition] = Currency.USD
                settings[DelayedPaymentMethodsSettingsDefinition] = true
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
            },
        )
    }
}
