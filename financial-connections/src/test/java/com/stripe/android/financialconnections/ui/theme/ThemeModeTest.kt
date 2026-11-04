package com.stripe.android.financialconnections.ui.theme

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ThemeMode
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.annotation.Config

@RunWith(RobolectricTestParameterInjector::class)
@Config(qualifiers = "notnight")
internal class ThemeModeTest {
    @Test
    fun `activity theme overrides the app theme only when requested`(
        @TestParameter themeMode: ThemeMode,
        @TestParameter appIsDark: Boolean,
    ) = runScenario(
        themeMode = themeMode,
        defaultNightMode = if (appIsDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO,
    ) { activity ->
        val expectedDark = when (themeMode) {
            ThemeMode.Automatic -> appIsDark
            ThemeMode.AlwaysLight -> false
            ThemeMode.AlwaysDark -> true
        }
        assertThat(activity.isDark()).isEqualTo(expectedDark)
    }

    @Test
    @Config(qualifiers = "night")
    fun `automatic follows the system when the app follows the system`() = runScenario(
        themeMode = ThemeMode.Automatic,
        defaultNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    ) { activity ->
        assertThat(activity.isDark()).isTrue()
    }

    private fun runScenario(
        themeMode: ThemeMode,
        defaultNightMode: Int,
        block: (AppCompatActivity) -> Unit,
    ) {
        val previousNightMode = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(defaultNightMode)
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        try {
            val activity = controller.get()
            activity.setTheme(R.style.StripeFinancialConnectionsDefaultTheme)
            activity.delegate.localNightMode = themeMode.toNightMode()
            controller.setup()

            block(activity)
            assertThat(AppCompatDelegate.getDefaultNightMode()).isEqualTo(defaultNightMode)
        } finally {
            controller.pause().stop().destroy()
            AppCompatDelegate.setDefaultNightMode(previousNightMode)
        }
    }

    private fun AppCompatActivity.isDark(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
