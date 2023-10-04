package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPayPal : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "paypal",
    ) { settings ->
        settings[CurrencySettingsDefinition] = Currency.GBP
    }

    @Test
    fun testPayPal() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testPayPalInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
