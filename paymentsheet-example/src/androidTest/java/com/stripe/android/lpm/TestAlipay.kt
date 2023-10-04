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
internal class TestAlipay : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "alipay",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.Klarna.code,
            PaymentMethod.Type.Affirm.code,
            PaymentMethod.Type.Alipay.code,
        )
    }

    @Test
    fun testAlipay() {
        testDriver.confirmNewOrGuestComplete(testParameters)
    }

    @Test
    fun testAlipayFailure() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "We are unable to authenticate your payment method. Please " +
                        "choose a different payment method and try again.",
                ),
            )
        )
    }

    @Test
    fun testAlipayInCustomFlow() {
        testDriver.confirmCustom(testParameters)
    }
}
