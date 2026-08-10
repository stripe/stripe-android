package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Suppress("RestrictedApi")
internal class CheckoutControllerSavedStateTest {
    @Test
    fun `handle restores state from the parent namespace`() {
        val savedState = CheckoutControllerSavedState(
            parentHandle = parentHandleWithValue("restored"),
            integrationName = INTEGRATION_NAME,
        )

        assertThat(savedState.handle.get<String>(VALUE_KEY)).isEqualTo("restored")
    }

    @Test
    fun `handle state is persisted through the parent`() {
        val parentHandle = SavedStateHandle()
        val savedState = CheckoutControllerSavedState(
            parentHandle = parentHandle,
            integrationName = INTEGRATION_NAME,
        )
        savedState.handle[VALUE_KEY] = "persisted"

        val restored = CheckoutControllerSavedState(
            parentHandle = parentHandle.simulateProcessDeath(),
            integrationName = INTEGRATION_NAME,
        )

        assertThat(restored.handle.get<String>(VALUE_KEY)).isEqualTo("persisted")
    }

    @Test
    fun `instances do not share a child handle`() {
        val parentHandle = SavedStateHandle()

        val first = CheckoutControllerSavedState(parentHandle, INTEGRATION_NAME)
        val second = CheckoutControllerSavedState(parentHandle, INTEGRATION_NAME)

        assertThat(first.handle).isNotSameInstanceAs(second.handle)
    }

    @Test
    fun `clear removes the namespace from the parent`() {
        val parentHandle = SavedStateHandle()
        val savedState = CheckoutControllerSavedState(parentHandle, INTEGRATION_NAME)
        savedState.handle[VALUE_KEY] = "value"
        parentHandle.savedStateProvider().saveState()

        savedState.clear()

        assertThat(parentHandle.keys()).doesNotContain(INTEGRATION_NAME)
    }

    @Test
    fun `clear prevents state restoration`() {
        val parentHandle = SavedStateHandle()
        val savedState = CheckoutControllerSavedState(parentHandle, INTEGRATION_NAME)
        savedState.handle[VALUE_KEY] = "value"

        savedState.clear()
        val restored = CheckoutControllerSavedState(
            parentHandle = parentHandle.simulateProcessDeath(),
            integrationName = INTEGRATION_NAME,
        )

        assertThat(restored.handle.get<String>(VALUE_KEY)).isNull()
    }

    private fun parentHandleWithValue(value: String): SavedStateHandle {
        val childHandle = SavedStateHandle(mapOf(VALUE_KEY to value))
        return SavedStateHandle(
            mapOf(INTEGRATION_NAME to childHandle.savedStateProvider().saveState())
        )
    }

    private fun SavedStateHandle.simulateProcessDeath(): SavedStateHandle {
        return SavedStateHandle.createHandle(savedStateProvider().saveState(), null)
    }

    private companion object {
        const val INTEGRATION_NAME = "integration_name"
        const val VALUE_KEY = "value"
    }
}
