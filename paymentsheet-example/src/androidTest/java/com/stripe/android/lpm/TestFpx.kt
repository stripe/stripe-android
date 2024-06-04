package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestFpx : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "fpx",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.MY
        settings[CurrencySettingsDefinition] = Currency.MYR
        settings[AutomaticPaymentMethodsSettingsDefinition] = true
    }

    @Test
    fun testFpx() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testFpxInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
