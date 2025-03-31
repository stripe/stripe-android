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
internal class TestAmazonPay : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "amazon_pay",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
    }

    @Test
    fun testAmazonPayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
