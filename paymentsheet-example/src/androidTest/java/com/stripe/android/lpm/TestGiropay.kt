package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestGiropay : BaseLpmTest() {
    private val giropay = newUser.copy(
        paymentMethod = lpmRepository.fromCode("giropay")!!,
    )

    @Test
    fun testGiropay() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = giropay,
        )
    }

    @Test
    fun testGiropayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = giropay,
        )
    }
}
