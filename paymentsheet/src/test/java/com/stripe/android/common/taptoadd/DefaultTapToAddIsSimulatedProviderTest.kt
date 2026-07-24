package com.stripe.android.common.taptoadd

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DefaultTapToAddIsSimulatedProviderTest {

    @Test
    fun `get returns true when test mode and application is debuggable`() {
        val provider = DefaultTapToAddIsSimulatedProvider(
            applicationContext = context(debuggable = true),
            isLiveModeProvider = { false },
        )

        assertThat(provider.get()).isTrue()
    }

    @Test
    fun `get returns false when live mode even if debuggable`() {
        val provider = DefaultTapToAddIsSimulatedProvider(
            applicationContext = context(debuggable = true),
            isLiveModeProvider = { true },
        )

        assertThat(provider.get()).isFalse()
    }

    @Test
    fun `get returns false when test mode but application is not debuggable`() {
        val provider = DefaultTapToAddIsSimulatedProvider(
            applicationContext = context(debuggable = false),
            isLiveModeProvider = { false },
        )

        assertThat(provider.get()).isFalse()
    }

    @Test
    fun `get returns false when live mode and not debuggable`() {
        val provider = DefaultTapToAddIsSimulatedProvider(
            applicationContext = context(debuggable = false),
            isLiveModeProvider = { true },
        )

        assertThat(provider.get()).isFalse()
    }

    private fun context(debuggable: Boolean): Context {
        val appInfo = ApplicationInfo().apply {
            flags = if (debuggable) ApplicationInfo.FLAG_DEBUGGABLE else 0
        }

        val context = mock<Context>()

        whenever(context.applicationContext).thenReturn(context)
        whenever(context.applicationInfo).thenReturn(appInfo)

        return context
    }
}
