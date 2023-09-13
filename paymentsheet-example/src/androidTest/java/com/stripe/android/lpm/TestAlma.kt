package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAlma : BaseLpmTest() {
    private val alma = newUser.copy(
        paymentMethod = lpmRepository.fromCode("alma")!!,
        currency = Currency.EUR,
        merchantCountryCode = "FR",
    )

    @Ignore("Complex authorization handling required")
    @Test
    fun testAlma() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = alma,
        )
    }

    @Test
    fun testAlmaInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = alma,
        )
    }
}
