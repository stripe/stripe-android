package com.stripe.android.uicore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class StripeThemeScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.fillMaxWidth(),
        includeStripeTheme = false,
    )

    @Test
    fun explicitDarkTheme() {
        snapshotTheme(isDark = true)
    }

    @Test
    fun explicitLightTheme() {
        snapshotTheme(isDark = false)
    }

    @Test
    fun systemTheme() {
        paparazziRule.snapshot {
            StripeTheme {
                ThemeContent()
            }
        }
    }

    private fun snapshotTheme(isDark: Boolean) {
        paparazziRule.snapshot {
            StripeTheme(
                isDark = isDark,
                colors = StripeThemeDefaults.colors(isDark),
                shapes = StripeThemeDefaults.shapes,
                typography = StripeThemeDefaults.typography,
                sectionSpacing = StripeThemeDefaults.sectionSpacing,
                sectionStyle = StripeThemeDefaults.sectionStyle,
                textFieldInsets = StripeThemeDefaults.textFieldInsets,
                iconStyle = StripeThemeDefaults.iconStyle,
            ) {
                ThemeContent()
            }
        }
    }

    @Composable
    private fun ThemeContent() {
        Surface(
            color = MaterialTheme.colors.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Payment details",
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.h6,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (MaterialTheme.stripeThemeIsDark) {
                        "Dark appearance"
                    } else {
                        "Light appearance"
                    },
                    color = MaterialTheme.stripeColors.subtitle,
                    style = MaterialTheme.typography.body2,
                )
            }
        }
    }
}
