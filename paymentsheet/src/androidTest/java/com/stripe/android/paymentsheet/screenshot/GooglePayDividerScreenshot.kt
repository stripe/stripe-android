package com.stripe.android.paymentsheet.screenshot

import androidx.compose.material.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import com.karumi.shot.ScreenshotTest
import com.stripe.android.paymentsheet.ui.GooglePayDividerUi
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.PaymentsThemeDefaults
import org.junit.Rule
import org.junit.Test

class GooglePayDividerScreenshot : ScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun googlePayDividerUiLight() {
        val colors = PaymentsThemeDefaults.colorsLight
        composeTestRule.setContent {
            PaymentsTheme(
                colors = PaymentsThemeDefaults.colorsLight
            ) {
                Surface(color = colors.materialColors.surface) {
                    GooglePayDividerUi()
                }
            }
        }
        compareScreenshot(composeTestRule)
    }

    @Test
    fun googlePayDividerUiDark() {
        val colors = PaymentsThemeDefaults.colorsDark
        composeTestRule.setContent {
            PaymentsTheme(
                colors = PaymentsThemeDefaults.colorsDark
            ) {
                Surface(color = colors.materialColors.surface) {
                    GooglePayDividerUi()
                }
            }
        }
        compareScreenshot(composeTestRule)
    }

    @Test
    fun googlePayDividerUiBorders() {
        composeTestRule.setContent {
            PaymentsTheme(
                shapes = PaymentsThemeDefaults.shapes.copy(borderStrokeWidth = 5.0f)
            ) {
                Surface {
                    GooglePayDividerUi()
                }
            }
        }
        compareScreenshot(composeTestRule)
    }

    @Test
    fun googlePayDividerUiBigFont() {
        composeTestRule.setContent {
            PaymentsTheme(
                typography = PaymentsThemeDefaults.typography.copy(fontSizeMultiplier = 1.3f)
            ) {
                Surface {
                    GooglePayDividerUi()
                }
            }
        }
        compareScreenshot(composeTestRule)
    }
}
