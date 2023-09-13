package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.IntentType
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
    fun testIdealSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = ideal.copy(
                delayed = DelayedPMs.On,
                automatic = Automatic.On,
                intentType = IntentType.PayWithSetup,
            ),
        )
    }

    @Test
    fun testIdealSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = ideal.copy(
                delayed = DelayedPMs.On,
                automatic = Automatic.On,
                intentType = IntentType.Setup,
            ),
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
