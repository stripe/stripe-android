package com.stripe.android.paymentsheet.screenshot

import androidx.compose.material.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import com.karumi.shot.ScreenshotTest
import com.stripe.android.paymentsheet.addresselement.AddressElementPrimaryButton
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.PaymentsThemeDefaults
import org.junit.Rule
import org.junit.Test

class AddressElementPrimaryButtonScreenshot : ScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addressPrimaryUiEnabledLight() {
        val colors = PaymentsThemeDefaults.colorsLight
        composeTestRule.setContent {
            PaymentsTheme(
                colors = PaymentsThemeDefaults.colorsLight
            ) {
                Surface(color = colors.materialColors.surface) {
                    AddressElementPrimaryButton(isEnabled = true) {
                    }
                }
            }
        }
        compareScreenshot(composeTestRule)
    }

    @Test
    fun addressPrimaryUiEnabledDark() {
        val colors = PaymentsThemeDefaults.colorsDark
        composeTestRule.setContent {
            PaymentsTheme(
                colors = PaymentsThemeDefaults.colorsDark
            ) {
                Surface(color = colors.materialColors.surface) {
                    AddressElementPrimaryButton(isEnabled = true) {
                    }
                }
            }
        }
        compareScreenshot(composeTestRule)
    }

    @Test
    fun addressPrimaryUiDisabledLight() {
        val colors = PaymentsThemeDefaults.colorsLight
        composeTestRule.setContent {
            PaymentsTheme(
                colors = PaymentsThemeDefaults.colorsLight
            ) {
                Surface(color = colors.materialColors.surface) {
                    AddressElementPrimaryButton(isEnabled = false) {}
                }
            }
        }
        compareScreenshot(composeTestRule)
    }

    @Test
    fun addressPrimaryUiDisabledDark() {
        val colors = PaymentsThemeDefaults.colorsDark
        composeTestRule.setContent {
            PaymentsTheme(
                colors = PaymentsThemeDefaults.colorsDark
            ) {
                Surface(color = colors.materialColors.surface) {
                    AddressElementPrimaryButton(isEnabled = false) {}
                }
            }
        }
        compareScreenshot(composeTestRule)
    }
}
