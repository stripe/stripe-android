package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.simulateProcessDeath
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Suppress("RestrictedApi")
internal class EmbeddedPaymentElementSavedStateTest {
    @Test
    fun `child state is saved and restored through the parent namespace`() {
        val parentHandle = SavedStateHandle()
        val savedState = EmbeddedPaymentElementSavedState(parentHandle, INTEGRATION_NAME)
        savedState.handle[VALUE_KEY] = "configured"

        val restored = EmbeddedPaymentElementSavedState(
            parentHandle = parentHandle.simulateProcessDeath(),
            integrationName = INTEGRATION_NAME,
        )

        assertThat(restored.handle.get<String>(VALUE_KEY)).isEqualTo("configured")
    }

    @Test
    fun `clear removes a saved namespace and stops saving subsequent child updates`() {
        val parentHandle = SavedStateHandle()
        val savedState = EmbeddedPaymentElementSavedState(parentHandle, INTEGRATION_NAME)
        savedState.handle[VALUE_KEY] = "configured"
        parentHandle.savedStateProvider().saveState()
        assertThat(parentHandle.keys()).contains(INTEGRATION_NAME)

        savedState.clear()
        savedState.handle[VALUE_KEY] = "after clear"
        val restoredParent = parentHandle.simulateProcessDeath()

        assertThat(parentHandle.keys()).doesNotContain(INTEGRATION_NAME)
        assertThat(restoredParent.keys()).doesNotContain(INTEGRATION_NAME)
        val restored = EmbeddedPaymentElementSavedState(restoredParent, INTEGRATION_NAME)
        assertThat(restored.handle.get<String>(VALUE_KEY)).isNull()
    }

    @Test
    fun `repeated creation saving and clearing does not accumulate namespaces`() {
        val parentHandle = SavedStateHandle()

        repeat(20) { index ->
            val integrationName = "integration_$index"
            val savedState = EmbeddedPaymentElementSavedState(parentHandle, integrationName)
            savedState.handle[VALUE_KEY] = "configured_$index"
            parentHandle.savedStateProvider().saveState()
            assertThat(parentHandle.keys()).containsExactly(integrationName)

            savedState.clear()

            assertThat(parentHandle.simulateProcessDeath().keys()).isEmpty()
        }

        assertThat(parentHandle.keys()).isEmpty()
    }

    @Test
    fun `clearing one namespace preserves the other integration and unrelated parent state`() {
        val parentHandle = SavedStateHandle(mapOf("merchant_state" to "preserved"))
        val first = EmbeddedPaymentElementSavedState(parentHandle, "first")
        val second = EmbeddedPaymentElementSavedState(parentHandle, "second")
        first.handle[VALUE_KEY] = "first selection"
        second.handle[VALUE_KEY] = "second selection"
        parentHandle.savedStateProvider().saveState()
        assertThat(first.handle.get<String>(VALUE_KEY)).isEqualTo("first selection")
        assertThat(second.handle.get<String>(VALUE_KEY)).isEqualTo("second selection")

        first.clear()
        val restoredParent = parentHandle.simulateProcessDeath()

        assertThat(restoredParent.keys()).containsExactly("second", "merchant_state")
        assertThat(restoredParent.get<String>("merchant_state")).isEqualTo("preserved")
        val restoredFirst = EmbeddedPaymentElementSavedState(restoredParent, "first")
        val restoredSecond = EmbeddedPaymentElementSavedState(restoredParent, "second")
        assertThat(restoredFirst.handle.get<String>(VALUE_KEY)).isNull()
        assertThat(restoredSecond.handle.get<String>(VALUE_KEY)).isEqualTo("second selection")
    }

    private companion object {
        const val INTEGRATION_NAME = "integration_name"
        const val VALUE_KEY = "value"
    }
}
