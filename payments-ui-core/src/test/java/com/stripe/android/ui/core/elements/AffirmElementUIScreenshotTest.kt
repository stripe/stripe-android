package com.stripe.android.ui.core.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeThemeIsDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class AffirmElementUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier.padding(16.dp),
    )

    @get:Rule
    val disableLuminanceBasedThemesRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.disableLuminanceBasedThemes,
        isEnabled = false,
    )

    @Test
    fun testDefaultColors(@TestParameter disableLuminanceBasedThemes: Boolean) {
        disableLuminanceBasedThemesRule.setEnabled(disableLuminanceBasedThemes)
        paparazziRule.snapshot {
            AffirmElementUI(modifier = Modifier)
        }
    }

    @Test
    fun testComponentColorOpposesTheme(@TestParameter disableLuminanceBasedThemes: Boolean) {
        disableLuminanceBasedThemesRule.setEnabled(disableLuminanceBasedThemes)
        paparazziRule.snapshot {
            val componentColor = if (MaterialTheme.stripeThemeIsDark) Color.White else Color.Black
            StripeTheme(
                colors = MaterialTheme.stripeColors.copy(component = componentColor),
            ) {
                AffirmElementUI(modifier = Modifier)
            }
        }
    }
}
