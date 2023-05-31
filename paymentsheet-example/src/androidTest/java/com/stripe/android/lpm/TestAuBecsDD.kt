package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.DelayedPMs
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAuBecsDD : BaseLpmTest() {
    private val auBecsDD = newUser.copy(
        paymentMethod = lpmRepository.fromCode("au_becs_debit")!!,
        delayed = DelayedPMs.On,
        merchantCountryCode = "AU",
        currency = Currency.AUD,
        authorizationAction = null,
    )

    @Test
    fun testAuBecsDD() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = auBecsDD,
        )
    }

    @Test
    fun testAuBecsDDInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = auBecsDD,
        )
    }
}
