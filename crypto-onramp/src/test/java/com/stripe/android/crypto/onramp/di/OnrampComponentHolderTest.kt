package com.stripe.android.crypto.onramp.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Suppress("RestrictedApi")
class OnrampComponentHolderTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        OnrampComponentHolder.reset()
    }

    @After
    fun tearDown() {
        OnrampComponentHolder.reset()
    }

    @Test
    fun `reopening retains the component and saves subsequent writes through the new host`() {
        val originalHost = SavedStateHandle()
        val original = OnrampComponentHolder.getOrCreate(application, originalHost)
        original.savedStateHandle["checkout"] = "before_reopening"

        val newHost = SavedStateHandle()
        val reopened = OnrampComponentHolder.getOrCreate(application, newHost)
        assertThat(reopened).isSameInstanceAs(original)
        reopened.savedStateHandle["checkout"] = "after_reopening"

        val restoredHost = restore(newHost)
        OnrampComponentHolder.reset()
        val restored = OnrampComponentHolder.getOrCreate(application, restoredHost)

        assertThat(restored.savedStateHandle.get<String>("checkout")).isEqualTo("after_reopening")
    }

    @Test
    fun `reusing the same host preserves saved state without nesting providers`() {
        val host = SavedStateHandle()
        val original = OnrampComponentHolder.getOrCreate(application, host)
        original.savedStateHandle["checkout"] = "pending"
        val reused = OnrampComponentHolder.getOrCreate(application, host)
        assertThat(reused).isSameInstanceAs(original)

        val restoredHost = restore(host)
        OnrampComponentHolder.reset()
        val restored = OnrampComponentHolder.getOrCreate(application, restoredHost)

        assertThat(restored.savedStateHandle.get<String>("checkout")).isEqualTo("pending")
    }

    @Test
    fun `host state stays separate from SDK state`() {
        val host = SavedStateHandle(mapOf("screen" to "merchant_screen"))
        val component = OnrampComponentHolder.getOrCreate(application, host)
        component.savedStateHandle["screen"] = "sdk_screen"

        val restoredHost = restore(host)
        OnrampComponentHolder.reset()
        val restored = OnrampComponentHolder.getOrCreate(application, restoredHost)

        assertThat(restoredHost.get<String>("screen")).isEqualTo("merchant_screen")
        assertThat(restored.savedStateHandle.get<String>("screen")).isEqualTo("sdk_screen")
    }

    private fun restore(handle: SavedStateHandle): SavedStateHandle {
        return SavedStateHandle.createHandle(handle.savedStateProvider().saveState(), null)
    }
}
