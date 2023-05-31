package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Shipping
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAfterpay : BaseLpmTest() {
    private val afterpay = newUser.copy(
        paymentMethod = lpmRepository.fromCode("afterpay_clearpay")!!,
        merchantCountryCode = "US",
        currency = Currency.USD,
        shipping = Shipping.OnWithDefaults,
    )

    @Test
    fun testAfterpay() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = afterpay,
        )
    }

    @Test
    fun testAfterpayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = afterpay,
        )
    }
}
