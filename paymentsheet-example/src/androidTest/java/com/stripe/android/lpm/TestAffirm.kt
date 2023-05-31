package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Shipping
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAffirm : BaseLpmTest() {
    private val affirm = newUser.copy(
        paymentMethod = lpmRepository.fromCode("affirm")!!,
        merchantCountryCode = "US",
        currency = Currency.USD,
        shipping = Shipping.On,
    )

    @Test
    fun testAffirmInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = affirm,
        )
    }
}
