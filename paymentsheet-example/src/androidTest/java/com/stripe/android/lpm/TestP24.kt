package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestP24 : BaseLpmTest() {
    private val p24 = newUser.copy(
        paymentMethod = lpmRepository.fromCode("p24")!!,
    )

    @Test
    fun testP24() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = p24,
        )
    }

    @Test
    fun testP24InCustomFlow() {
        testDriver.confirmCustom(
            testParameters = p24,
        )
    }
}
