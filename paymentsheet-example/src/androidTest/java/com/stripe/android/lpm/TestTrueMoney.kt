package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestTrueMoney : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "truemoney",
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.TH
        settings[CurrencySettingsDefinition] = Currency.THB
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.TrueMoney
        ).joinToString(",")
    }

    @Test
    fun testTrueMoney() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testTrueMoneyPaymentWithSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            },
        )
    }

    @Test
    fun testTrueMoneySetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            },
        )
    }
}
