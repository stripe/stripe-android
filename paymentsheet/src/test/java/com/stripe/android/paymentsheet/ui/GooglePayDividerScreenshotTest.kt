package com.stripe.android.paymentsheet.ui

import app.cash.paparazzi.Paparazzi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.utils.ComponentTestConfig
import com.stripe.android.utils.ComponentTestConfigProvider
import com.stripe.android.utils.PaymentSheetTestTheme
import com.stripe.android.utils.createPaparazzi
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(TestParameterInjector::class)
class GooglePayDividerScreenshotTest(
    @TestParameter(
        valuesProvider = ComponentTestConfigProvider::class,
    ) private val testConfig: ComponentTestConfig,
) {

    @get:Rule
    val paparazzi: Paparazzi = testConfig.createPaparazzi()

    @Test
    fun testThings() {
        paparazzi.snapshot {
            PaymentSheetTestTheme(testConfig) {
                GooglePayDividerUi()
            }
        }
    }
}
