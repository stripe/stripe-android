package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestIdeal : BaseLpmTest() {
    private val ideal = newUser.copy(
        paymentMethod = lpmRepository.fromCode("ideal")!!,
    )

    @Test
    fun testIdeal() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = ideal,
        )
    }

    @Test
    fun testIdealInCustomFlow() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("ideal")!!,
            )
        )
    }
}
