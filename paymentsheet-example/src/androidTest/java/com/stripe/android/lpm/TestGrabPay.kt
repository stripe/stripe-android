package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestGrabPay : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "grabpay",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.SG
        settings[CurrencySettingsDefinition] = Currency.SGD
    }

    @Test
    fun testGrabPay() {
        testDriver.confirmNewOrGuestComplete(testParameters)
    }

    @Test
    fun testGrabPayFailure() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "We are unable to authenticate your payment method. Please " +
                        "choose a different payment method and try again.",
                ),
            )
        )
    }

    @Test
    fun testGrabPayInCustomFlow() {
        testDriver.confirmCustom(testParameters)
    }
}
