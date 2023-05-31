package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestKlarna : BaseLpmTest() {
    private val klarna = newUser.copy(
        paymentMethod = lpmRepository.fromCode("klarna")!!,
        currency = Currency.USD,
        merchantCountryCode = "US",
    )

    @Ignore("Complex authorization handling required")
    @Test
    fun testKlarna() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = klarna,
        )
    }

    @Ignore("Complex authorization handling required")
    @Test
    fun testKlarnaInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = klarna,
        )
    }
}
