package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPayPal : BaseLpmTest() {
    private val paypal = newUser.copy(
        paymentMethod = lpmRepository.fromCode("paypal")!!,
        currency = Currency.GBP,
        merchantCountryCode = "GB",
    )

    @Test
    fun testPayPal() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = paypal,
        )
    }

    @Test
    fun testPayPalInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = paypal,
        )
    }
}
