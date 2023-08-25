package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAmazonPay : BaseLpmTest() {
    private val amazonPay = newUser.copy(
        paymentMethod = lpmRepository.fromCode("amazon_pay")!!,
        currency = Currency.USD,
        merchantCountryCode = "US",
    )

    @Ignore("Complex authorization handling required")
    @Test
    fun testAmazonPay() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = amazonPay,
        )
    }

    @Test
    fun testAmazonPayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = amazonPay,
        )
    }
}
