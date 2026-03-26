package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPayByBank : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "pay_by_bank",
        authorizationAction = AuthorizeAction.PayByBank,
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.GB
        settings[CurrencySettingsDefinition] = Currency.GBP
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.PayByBank
        ).joinToString(",")
    }

    @Test
    fun testPayByBank() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }
}
