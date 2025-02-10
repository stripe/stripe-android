package com.stripe.android.lpm

import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
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
}
