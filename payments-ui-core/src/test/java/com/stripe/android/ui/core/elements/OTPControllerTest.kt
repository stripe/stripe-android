package com.stripe.android.ui.core.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OTPControllerTest {
    private val otpConfig = OTPController()

    @Test
    fun `verify only numbers are allowed`() = runTest {
        otpConfig.fieldValue.test {
            // Entering letters doesn't emit any value after the first, empty one
            otpConfig.onValueChanged(0, "a")
            idleLooper()
            assertThat(awaitItem()).isEmpty()

            otpConfig.onValueChanged(1, "b")
            idleLooper()

            otpConfig.onValueChanged(2, "c")
            idleLooper()

            // Entering a number emits new value
            otpConfig.onValueChanged(3, "5")
            idleLooper()
            assertThat(awaitItem()).isEqualTo("5")
        }
    }

    @Test
    fun `verify entering multiple characters uses them all`() = runTest {
        otpConfig.fieldValue.test {
            assertThat(awaitItem()).isEmpty()
            otpConfig.onValueChanged(0, "123456")
            idleLooper()

            skipItems(5)

            assertThat(awaitItem()).isEqualTo("123456")
        }
    }
}
