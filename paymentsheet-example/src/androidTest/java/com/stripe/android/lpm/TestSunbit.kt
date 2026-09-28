package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.AmountSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestSunbit : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "sunbit",
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.US
        settings[CurrencySettingsDefinition] = Currency.USD
        // Sunbit has a minimum transaction amount of $60
        settings[AmountSettingsDefinition] = "6099"
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.Sunbit
        ).joinToString(",")
    }

    @Test
    fun testSunbit() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }
}
