package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestRevolutPay : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "revolut_pay",
    ) { settings ->
        settings[CurrencySettingsDefinition] = Currency.GBP
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf("card", "revolut_pay")
    }

    @Ignore("Requires complex auth handling")
    @Test
    fun testRevolutPay_Success() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters
        )
    }

    @Ignore("Requires complex auth handling")
    @Test
    fun testRevolutPay_Fail() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "The customer declined this payment.",
                ),
            ),
        )
    }

    @Ignore("Requires complex auth handling")
    @Test
    fun testRevolutPay_Cancel() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel
            ),
        )
    }

    @Ignore("Requires complex auth handling")
    @Test
    fun testRevolutPayWithSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            }
        )
    }

    @Ignore("Requires complex auth handling")
    @Test
    fun testRevolutPayWithSetupIntent() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            }
        )
    }

    @Ignore("Requires complex auth handling")
    @Test
    fun testRevolutPayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters
        )
    }
}
