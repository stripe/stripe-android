package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestScalapay : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "scalapay",
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.IT
        settings[CurrencySettingsDefinition] = Currency.EUR
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.Scalapay
        ).joinToString(",")
    }

    @Test
    fun testScalapay() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }
}
