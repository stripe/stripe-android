package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPayByBank : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "pay_by_bank",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.GB
        settings[CurrencySettingsDefinition] = Currency.GBP
    }

    @Test
    fun testPayByBank() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testPayByBankInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}