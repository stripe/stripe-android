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
internal class TestKonbini : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "konbini",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.JP
        settings[CurrencySettingsDefinition] = Currency.JPY
        settings[CustomerSettingsDefinition] = CustomerType.GUEST
        settings[DelayedPaymentMethodsSettingsDefinition] = true
        settings[DefaultBillingAddressSettingsDefinition] = false
    }.copy(
        authorizationAction = AuthorizeAction.DisplayQrCode,
    )

    @Test
    fun testKonbini() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testKonbiniInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
