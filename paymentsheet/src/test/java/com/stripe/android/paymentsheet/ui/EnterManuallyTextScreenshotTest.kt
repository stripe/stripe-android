package com.stripe.android.paymentsheet.ui

import app.cash.paparazzi.Paparazzi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.paymentsheet.addresselement.EnterManuallyText
import com.stripe.android.utils.screenshots.ComponentTestConfig
import com.stripe.android.utils.screenshots.ComponentTestConfigProvider
import com.stripe.android.utils.screenshots.PaymentSheetTestTheme
import com.stripe.android.utils.screenshots.createPaparazzi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class EnterManuallyTextScreenshotTest(
    @TestParameter(
        valuesProvider = ComponentTestConfigProvider::class,
    ) private val testConfig: ComponentTestConfig,
) {

    @get:Rule
    val paparazzi: Paparazzi = testConfig.createPaparazzi()

    @Test
    fun testDefault() {
        paparazzi.snapshot {
            PaymentSheetTestTheme(testConfig) {
                EnterManuallyText(onClick = {})
            }
        }
    }
}
