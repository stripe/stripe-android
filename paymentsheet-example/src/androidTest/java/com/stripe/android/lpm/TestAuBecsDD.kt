package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAuBecsDD : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "au_becs_debit",
    ) { settings ->
        settings[DelayedPaymentMethodsSettingsDefinition] = true
        settings[CountrySettingsDefinition] = Country.AU
        settings[CurrencySettingsDefinition] = Currency.AUD
    }.copy(
        authorizationAction = null,
    )

    @Test
    fun testAuBecsDD() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testAuBecsDDInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }

    @Test
    fun testAuBecsDDSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[AutomaticPaymentMethodsSettingsDefinition] = true
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            }
        )
    }

    @Test
    fun testAuBecsDDSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[AutomaticPaymentMethodsSettingsDefinition] = true
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            }
        )
    }
}
