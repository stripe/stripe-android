package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestOxxo : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "oxxo",
    ) { settings ->
        settings[CustomerSettingsDefinition] = CustomerType.GUEST
        settings[CountrySettingsDefinition] = Country.MX
        settings[CurrencySettingsDefinition] = Currency.MXN
        settings[DelayedPaymentMethodsSettingsDefinition] = true
        settings[DefaultBillingAddressSettingsDefinition] = false
    }.copy(
        authorizationAction = AuthorizeAction.DisplayQrCode,
    )

    @Test
    fun testOxxo() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testOxxoInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
