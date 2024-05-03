package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestEps : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "eps",
    )

    @Test
    fun testEps() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testEpsInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
