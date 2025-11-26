package com.stripe.android.lpm

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestUpi : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "upi",
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.IN
        settings[CurrencySettingsDefinition] = Currency.INR
    }.copy(
        authorizationAction = null,
    )

    @Test
    fun testUpi() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters
        ) {
            rules.compose.onNodeWithText("UPI ID").apply {
                performTextInput(
                    "payment.success@stripeupi"
                )
            }
        }
    }
}
