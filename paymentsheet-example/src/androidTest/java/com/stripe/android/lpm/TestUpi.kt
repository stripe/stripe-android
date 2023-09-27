package com.stripe.android.lpm

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestUpi : BaseLpmTest() {
    private val upi = newUser.copy(
        paymentMethod = lpmRepository.fromCode("upi")!!,
        currency = Currency.INR,
        merchantCountryCode = "IN",
        authorizationAction = null,
    )

    @Test
    fun testUpi() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = upi,
            populateCustomLpmFields = {
                rules.compose.onNodeWithText("UPI ID").apply {
                    performTextInput(
                        "payment.success@stripeupi"
                    )
                }
            }
        )
    }

    @Test
    fun testUpiInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = upi,
            populateCustomLpmFields = {
                rules.compose.onNodeWithText("UPI ID").apply {
                    performTextInput(
                        "payment.success@stripeupi"
                    )
                }
            }
        )
    }
}
