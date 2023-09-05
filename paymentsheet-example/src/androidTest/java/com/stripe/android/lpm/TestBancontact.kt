package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.IntentType
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
    fun testBancontactSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = bancontact.copy(
                delayed = DelayedPMs.On,
                automatic = Automatic.On,
                intentType = IntentType.PayWithSetup,
            ),
        )
    }

    @Test
    fun testBancontactInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = bancontact,
        )
    }
}
