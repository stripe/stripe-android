package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPayNow : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "paynow",
        authorizationAction = AuthorizeAction.ShowQrCodeThenPoll,
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.SG
        settings[CurrencySettingsDefinition] = Currency.SGD
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.PayNow
        ).joinToString(",")
    }

    @Test
    fun testPayNow() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }
}
