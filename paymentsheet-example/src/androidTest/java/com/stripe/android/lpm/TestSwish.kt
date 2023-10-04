package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.GooglePaySettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestSwish : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "swish",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.FR
        settings[CurrencySettingsDefinition] = Currency.SEK
        settings[DelayedPaymentMethodsSettingsDefinition] = true
        settings[GooglePaySettingsDefinition] = false
    }

    @Test
    fun testSwish() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testSwishInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
