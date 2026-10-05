package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPix : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "pix",
        authorizationAction = AuthorizeAction.ShowQrCodeThenPoll,
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.US
        settings[CurrencySettingsDefinition] = Currency.BRL
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.Pix,
        ).joinToString(",")
    }

    @Test
    fun testPixPayment() {
        confirmPix(testParameters)
    }

    @Test
    fun testPixPaymentWithSetup() {
        confirmPix(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            }
        )
    }

    @Test
    fun testPixSetup() {
        confirmPix(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            }
        )
    }

    private fun confirmPix(testParameters: TestParameters) {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            populateCustomLpmFields = {
                populateBrazilianTaxId()
            },
        )
    }
}
