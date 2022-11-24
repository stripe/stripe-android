package com.stripe.android.paymentsheet.ui

import app.cash.paparazzi.Paparazzi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.paymentsheet.addresselement.AddressElementPrimaryButton
import com.stripe.android.utils.screenshots.ComponentTestConfig
import com.stripe.android.utils.screenshots.ComponentTestConfigProvider
import com.stripe.android.utils.screenshots.PaymentSheetTestTheme
import com.stripe.android.utils.screenshots.createPaparazzi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BUTTON_TEXT = "Save Address"

@RunWith(TestParameterInjector::class)
class AddressElementPrimaryButtonScreenshotTest(
    @TestParameter(
        valuesProvider = ComponentTestConfigProvider::class,
    ) private val testConfig: ComponentTestConfig,
) {

    @get:Rule
    val paparazzi: Paparazzi = testConfig.createPaparazzi()

    @Test
    fun testEnabled() {
        paparazzi.snapshot {
            PaymentSheetTestTheme(testConfig) {
                AddressElementPrimaryButton(isEnabled = true, BUTTON_TEXT, onButtonClick = {})
            }
        }
    }

    @Test
    fun testDisabled() {
        paparazzi.snapshot {
            PaymentSheetTestTheme(testConfig) {
                AddressElementPrimaryButton(isEnabled = false, BUTTON_TEXT, onButtonClick = {})
            }
        }
    }
}
