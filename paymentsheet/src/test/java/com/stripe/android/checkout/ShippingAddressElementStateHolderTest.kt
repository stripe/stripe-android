package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.simulateProcessDeath
import org.junit.Test

internal class ShippingAddressElementStateHolderTest {
    @Test
    fun `presentation state survives process death`() {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = ShippingAddressElementStateHolder(savedStateHandle)
        stateHolder.isPresenting = true
        stateHolder.updaterKey = "updater-key"

        val restoredStateHolder = ShippingAddressElementStateHolder(
            savedStateHandle = savedStateHandle.simulateProcessDeath(),
        )

        assertThat(restoredStateHolder.isPresenting).isTrue()
        assertThat(restoredStateHolder.updaterKey).isEqualTo("updater-key")
    }
}
