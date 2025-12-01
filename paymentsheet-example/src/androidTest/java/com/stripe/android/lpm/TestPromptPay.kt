package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPromptPay : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "promptpay",
        authorizationAction = AuthorizeAction.ShowQrCodeThenPoll,
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.TH
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.PromptPay
        ).joinToString(",")
    }

    @Test
    fun testPromptPay() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }
}
