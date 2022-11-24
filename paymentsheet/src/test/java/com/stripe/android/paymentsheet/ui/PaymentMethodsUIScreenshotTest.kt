package com.stripe.android.paymentsheet.ui

import app.cash.paparazzi.Paparazzi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.MockPaymentMethodsFactory
import com.stripe.android.utils.screenshots.ComponentTestConfig
import com.stripe.android.utils.screenshots.ComponentTestConfigProvider
import com.stripe.android.utils.screenshots.PaymentSheetTestTheme
import com.stripe.android.utils.screenshots.createPaparazzi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PaymentMethodsUIScreenshotTest(
    @TestParameter(
        valuesProvider = ComponentTestConfigProvider::class,
    ) private val testConfig: ComponentTestConfig,
) {

    @get:Rule
    val paparazzi: Paparazzi = testConfig.createPaparazzi()

    private val paymentMethods: List<LpmRepository.SupportedPaymentMethod> by lazy {
        MockPaymentMethodsFactory.create()
    }

    @Test
    fun testInitialState() {
        paparazzi.snapshot {
            PaymentSheetTestTheme(testConfig) {
                PaymentMethodsUI(
                    paymentMethods = paymentMethods,
                    selectedIndex = 0,
                    isEnabled = true,
                    onItemSelectedListener = {}
                )
            }
        }
    }

    @Test
    fun testScrolledToEnd() {
        paparazzi.snapshot {
            PaymentSheetTestTheme(testConfig) {
                PaymentMethodsUI(
                    paymentMethods = paymentMethods,
                    selectedIndex = 3,
                    isEnabled = true,
                    onItemSelectedListener = {}
                )
            }
        }
    }

    @Test
    fun testDisabled() {
        paparazzi.snapshot {
            PaymentSheetTestTheme(testConfig) {
                PaymentMethodsUI(
                    paymentMethods = paymentMethods,
                    selectedIndex = 0,
                    isEnabled = false,
                    onItemSelectedListener = {}
                )
            }
        }
    }
}
