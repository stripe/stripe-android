package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPromptPay : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = PaymentMethod.Type.PromptPay.code,
        authorizationAction = AuthorizeAction.DisplayQrCode,
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.TH
        settings[CurrencySettingsDefinition] = Currency.THB
    }

    @Test
    fun testPromptPay_Success() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testPromptPayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
