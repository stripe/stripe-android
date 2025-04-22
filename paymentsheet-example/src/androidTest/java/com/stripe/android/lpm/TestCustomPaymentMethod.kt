package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomPaymentMethodPlaygroundType
import com.stripe.android.paymentsheet.example.playground.settings.CustomPaymentMethodsSettingDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DEFAULT_CUSTOM_PAYMENT_METHOD_ID
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodOrderSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCustomPaymentMethod : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = DEFAULT_CUSTOM_PAYMENT_METHOD_ID,
        executeInNightlyRun = true,
    ) { settings ->
        settings[CustomerSettingsDefinition] = CustomerType.GUEST
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[CustomPaymentMethodsSettingDefinition] = CustomPaymentMethodPlaygroundType.On
        settings[PaymentMethodOrderSettingsDefinition] = DEFAULT_CUSTOM_PAYMENT_METHOD_ID
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
        ).joinToString(",")
    }

    @Test
    fun testCustomPaymentMethod_Success() {
        testDriver.confirmCustomPaymentMethodSuccess(testParameters)
    }

    @Test
    fun testCustomPaymentMethod_Cancel() {
        testDriver.confirmCustomPaymentMethodCanceled(testParameters)
    }

    @Test
    fun testCustomPaymentMethod_Fail() {
        testDriver.confirmCustomPaymentMethodFailed(testParameters)
    }
}
