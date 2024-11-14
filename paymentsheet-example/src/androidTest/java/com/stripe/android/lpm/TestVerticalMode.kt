package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Layout
import com.stripe.android.paymentsheet.example.playground.settings.LayoutSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestVerticalMode : BasePlaygroundTest() {
    private val cardTestParameters = TestParameters.create(
        paymentMethodCode = "card",
        authorizationAction = null,
        executeInNightlyRun = true,
    ).copy(
        saveForFutureUseCheckboxVisible = true,
    ).copyPlaygroundSettings { settings ->
        settings[LayoutSettingsDefinition] = Layout.VERTICAL
    }

    private val cashAppTestParameters = TestParameters.create(
        paymentMethodCode = "cashapp",
        executeInNightlyRun = true,
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.CashAppPay
        ).joinToString(",")
        settings[LayoutSettingsDefinition] = Layout.VERTICAL
    }

    @Test
    fun testCard() {
        testDriver.confirmNewOrGuestComplete(
            cardTestParameters,
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCashAppPayWithSetupIntent() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = cashAppTestParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            },
            values = null, // We don't show the form for cash app.
        )
    }
}
