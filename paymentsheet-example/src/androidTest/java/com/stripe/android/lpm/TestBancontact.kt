package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestBancontact : BaseLpmTest() {
    private val bancontact = newUser.copy(
        paymentMethod = lpmRepository.fromCode("bancontact")!!,
    )

    @Test
    fun testBancontact() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = bancontact,
        )
    }

    @Test
    fun testBancontactInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = bancontact,
        )
    }
}
