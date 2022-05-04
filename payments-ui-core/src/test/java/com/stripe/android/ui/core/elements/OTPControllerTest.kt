package com.stripe.android.ui.core.elements

import androidx.lifecycle.asLiveData
import com.stripe.android.utils.TestUtils.idleLooper
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OTPControllerTest {
    private val otpConfig = OTPController()

    @Test
    fun `verify only numbers are allowed`() {
        val emittedValues = mutableListOf<String>()
        otpConfig.fieldValue.asLiveData().observeForever {
            emittedValues.add(it)
        }

        // Entering letters doesn't emit any value after the first, empty one
        otpConfig.onValueChanged(0, "a")
        idleLooper()
        assertThat(emittedValues).hasSize(1)
        assertThat(emittedValues.last()).isEmpty()

        otpConfig.onValueChanged(1, "b")
        idleLooper()
        assertThat(emittedValues).hasSize(1)

        otpConfig.onValueChanged(2, "c")
        idleLooper()
        assertThat(emittedValues).hasSize(1)

        // Entering a number emits new value
        otpConfig.onValueChanged(3, "5")
        idleLooper()
        assertThat(emittedValues).hasSize(2)
        assertThat(emittedValues.last()).isEqualTo("5")
    }

    @Test
    fun `verify entering multiple characters uses them all`() {
        val emittedValues = mutableListOf<String>()
        otpConfig.fieldValue.asLiveData().observeForever {
            emittedValues.add(it)
        }

        otpConfig.onValueChanged(0, "123456")
        idleLooper()
        assertThat(emittedValues.last()).isEqualTo("123456")
    }
}
