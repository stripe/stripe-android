package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPayNow : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "paynow",
        authorizationAction = AuthorizeAction.DisplayQrCode
    ) { settings ->
        settings[CustomerSettingsDefinition] = CustomerType.GUEST
        settings[CountrySettingsDefinition] = Country.SG
        settings[CurrencySettingsDefinition] = Currency.SGD
        settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.Off
        settings[AutomaticPaymentMethodsSettingsDefinition] = true
    }

    @Test
    fun testPayNow() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testPayNowInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
