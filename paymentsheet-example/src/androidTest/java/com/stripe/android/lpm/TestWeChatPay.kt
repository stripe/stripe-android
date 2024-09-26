package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestWeChatPay : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "wechat_pay",
        requiresBrowser = false,
    ) { settings ->
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.WeChatPay
        ).joinToString(",")
        settings[CountrySettingsDefinition] = Country.CN
        settings[CurrencySettingsDefinition] = Currency.EUR
        settings[CustomerSettingsDefinition] = CustomerType.GUEST
    }

    @Test
    fun testWeChatPay() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testWeChatPayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
