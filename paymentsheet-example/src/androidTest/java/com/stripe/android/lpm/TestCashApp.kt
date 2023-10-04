package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
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
internal class TestCashApp : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "cashapp",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf("card", "cashapp")
    }

    @Test
    fun testCashAppPay_Success() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testCashAppPay_Fail() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "The customer declined this payment.",
                ),
            ),
        )
    }

    @Test
    fun testCashAppPay_Cancel() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
        )
    }

    @Test
    fun testCashAppPayWithSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            }
        )
    }

    @Test
    fun testCashAppPayWithSetupIntent() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            }
        )
    }

    @Test
    fun testCashAppPayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
