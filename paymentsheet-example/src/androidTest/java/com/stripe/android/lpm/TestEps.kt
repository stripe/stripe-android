package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestEps : BaseLpmTest() {
    private val eps = newUser.copy(
        paymentMethod = lpmRepository.fromCode("eps")!!,
    )

    @Test
    fun testEps() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = eps,
        )
    }

    @Test
    fun testEpsInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = eps,
        )
    }
}
