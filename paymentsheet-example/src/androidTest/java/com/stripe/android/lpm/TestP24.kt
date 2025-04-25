package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestP24 : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "p24",
    )

    @Test
    fun testP24() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    fun testP24InCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
