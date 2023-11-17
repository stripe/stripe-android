package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestBancontact : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "bancontact",
    )

    @Test
    fun testBancontact() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testBancontactSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[DelayedPaymentMethodsSettingsDefinition] = true
                settings[AutomaticPaymentMethodsSettingsDefinition] = true
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            },
        )
    }

    @Test
    fun testBancontactSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[DelayedPaymentMethodsSettingsDefinition] = true
                settings[AutomaticPaymentMethodsSettingsDefinition] = true
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            },
        )
    }

    @Test
    fun testBancontactInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
