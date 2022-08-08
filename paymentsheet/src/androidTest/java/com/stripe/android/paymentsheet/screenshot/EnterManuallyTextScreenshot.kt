package com.stripe.android.paymentsheet.screenshot

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.karumi.shot.ScreenshotTest
import com.stripe.android.paymentsheet.addresselement.EnterManuallyText
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.PaymentsThemeDefaults
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore("Flakes on CI, need to investigate.")
class EnterManuallyTextScreenshot : ScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun enterManuallyUiLight() {
        val colors = PaymentsThemeDefaults.colorsLight
        composeTestRule.setContent {
            PaymentsTheme(
                colors = PaymentsThemeDefaults.colorsLight
            ) {
                Surface(color = colors.materialColors.surface) {
                    EnterManuallyText {}
                }
            }
        }
        compareScreenshot(composeTestRule)
    }

    @Test
    fun enterManuallyUiDark() {
        val colors = PaymentsThemeDefaults.colorsDark
        composeTestRule.setContent {
            PaymentsTheme(
                colors = PaymentsThemeDefaults.colorsDark
            ) {
                Surface(color = colors.materialColors.surface) {
                    EnterManuallyText {}
                }
            }
        }
        compareScreenshot(composeTestRule)
    }

    @Test
    fun enterManuallyUiThemed() {
        composeTestRule.setContent {
            PaymentsTheme(
                colors = PaymentsThemeDefaults.colorsLight.copy(
                    materialColors = MaterialTheme.colors.copy(primary = Color.Red)
                ),
                typography = PaymentsThemeDefaults.typography.copy(fontSizeMultiplier = 1.3f)
            ) {
                Surface {
                    EnterManuallyText {}
                }
            }
        }
        compareScreenshot(composeTestRule)
    }
}
