package com.stripe.android.uicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class StripeThemeNightModeOverrideTest {

    @After
    fun tearDown() {
        StripeTheme.nightModeOverride = null
    }

    @Test
    fun `isDarkTheme follows the system flag when override is null`() {
        StripeTheme.nightModeOverride = null

        assertThat(StripeTheme.isDarkTheme(systemInDarkTheme = true)).isTrue()
        assertThat(StripeTheme.isDarkTheme(systemInDarkTheme = false)).isFalse()
    }

    @Test
    fun `isDarkTheme forces light regardless of the system flag when override is false`() {
        StripeTheme.nightModeOverride = false

        assertThat(StripeTheme.isDarkTheme(systemInDarkTheme = true)).isFalse()
        assertThat(StripeTheme.isDarkTheme(systemInDarkTheme = false)).isFalse()
    }

    @Test
    fun `isDarkTheme forces dark regardless of the system flag when override is true`() {
        StripeTheme.nightModeOverride = true

        assertThat(StripeTheme.isDarkTheme(systemInDarkTheme = true)).isTrue()
        assertThat(StripeTheme.isDarkTheme(systemInDarkTheme = false)).isTrue()
    }

    @Test
    @Config(qualifiers = "night")
    fun `Context isSystemDarkTheme forces light on a dark device when override is false`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Sanity check - without an override, the night-qualified device reports dark.
        assertThat(context.isSystemDarkTheme()).isTrue()

        StripeTheme.nightModeOverride = false

        assertThat(context.isSystemDarkTheme()).isFalse()
    }

    @Test
    fun `Context isSystemDarkTheme forces dark on a light device when override is true`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Sanity check - without an override, the light device reports light.
        assertThat(context.isSystemDarkTheme()).isFalse()

        StripeTheme.nightModeOverride = true

        assertThat(context.isSystemDarkTheme()).isTrue()
    }
}
