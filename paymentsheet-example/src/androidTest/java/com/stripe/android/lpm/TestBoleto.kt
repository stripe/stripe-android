package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestBoleto : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "boleto",
    ) { settings ->
        settings[CustomerSettingsDefinition] = CustomerType.GUEST
        settings[CountrySettingsDefinition] = Country.BR
        settings[CurrencySettingsDefinition] = Currency.BRL
        settings[DelayedPaymentMethodsSettingsDefinition] = true
        settings[DefaultBillingAddressSettingsDefinition] = false
    }.copy(
        authorizationAction = AuthorizeAction.DisplayQrCode,
    )

    private val boletoValues = FieldPopulator.Values().copy(
        zip = "76600-000",
        state = "GO",
    )

    @Test
    fun testBoleto() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            values = boletoValues,
        )
    }

    @Test
    fun testBoletoSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            },
            values = boletoValues,
        )
    }

    @Test
    fun testBoletoSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            },
            values = boletoValues,
        )
    }

    @Test
    fun testBoletoInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
            values = boletoValues,
        )
    }
}
