package com.stripe.android.paymentsheet.screenshot

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.karumi.shot.ScreenshotTest
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.paymentsheet.utils.MockPaymentMethodsFactory
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.PaymentsThemeDefaults
import org.junit.Rule
import org.junit.Test

class PaymentMethodsUIScreenshot : ScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPaymentMethodsCarouselWithInitialScrollState() {
        val colors = PaymentsThemeDefaults.colorsLight
        val paymentMethods = MockPaymentMethodsFactory.create()

        composeTestRule.setContent {
            PaymentsTheme(colors = PaymentsThemeDefaults.colorsLight) {
                Surface(
                    color = colors.materialColors.surface,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    PaymentMethodsUI(
                        paymentMethods = paymentMethods,
                        selectedIndex = 0,
                        isEnabled = true,
                        onItemSelectedListener = {}
                    )
                }
            }
        }
        compareScreenshot(composeTestRule)
    }

    @Test
    fun testPaymentMethodsCarouselScrolledToTheEnd() {
        val colors = PaymentsThemeDefaults.colorsLight
        val paymentMethods = MockPaymentMethodsFactory.create()

        composeTestRule.setContent {
            PaymentsTheme(colors = PaymentsThemeDefaults.colorsLight) {
                Surface(
                    color = colors.materialColors.surface,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    PaymentMethodsUI(
                        paymentMethods = paymentMethods,
                        selectedIndex = 3,
                        isEnabled = true,
                        onItemSelectedListener = {}
                    )
                }
            }
        }
        compareScreenshot(composeTestRule)
    }
}
