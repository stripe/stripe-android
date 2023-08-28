package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestFpx : BaseLpmTest() {
    private val fpx = newUser.copy(
        paymentMethod = lpmRepository.fromCode("fpx")!!,
    )

    @Test
    fun testFpx() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = fpx,
        )
    }

    @Test
    fun testFpxInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = fpx,
        )
    }
}
