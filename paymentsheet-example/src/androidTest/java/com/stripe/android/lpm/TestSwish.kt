package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestSwish : BaseLpmTest() {

    private val swishUser = newUser.copy(
        paymentMethod = lpmRepository.fromCode("swish")!!,
        currency = Currency.SEK,
        merchantCountryCode = "FR",
        delayed = DelayedPMs.On,
        googlePayState = GooglePayState.Off,
    )

    @Test
    fun testSwish() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = swishUser,
        )
    }

    @Test
    fun testSwishInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = swishUser,
        )
    }
}
