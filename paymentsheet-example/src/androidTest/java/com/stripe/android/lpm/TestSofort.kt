package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.DelayedPMs
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestSofort : BaseLpmTest() {
    private val sofort = newUser.copy(
        paymentMethod = lpmRepository.fromCode("sofort")!!,
        currency = Currency.EUR,
        merchantCountryCode = "GB",
        delayed = DelayedPMs.On,
    )

    @Test
    fun testSofort() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = sofort,
        )
    }

    @Test
    fun testSofortInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = sofort,
        )
    }
}
