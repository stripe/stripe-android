package com.stripe.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.SystemAppearance.Dark
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.utils.MockPaymentMethodsFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PaymentMethodsUIScreenshotTest(
    @TestParameter(valuesProvider = PaymentSheetTestConfigProvider::class) val testConfig: PaymentSheetTestConfig
) {

    @get:Rule
    val paparazzi: Paparazzi = testConfig.createPaparazzi()

    @Test
    fun `Renders correctly when scrolled to the beginning`() {
        paparazzi.snapshot {
            PaymentMethodsUITestTheme {
                PaymentMethodsUI(
                    paymentMethods = MockPaymentMethodsFactory.create(),
                    selectedIndex = 0,
                    isEnabled = true,
                    onItemSelectedListener = {}
                )
            }
        }
    }

    @Test
    fun `Renders correctly when scrolled to the end`() {
        val paymentMethods = MockPaymentMethodsFactory.create()
        paparazzi.snapshot {
            PaymentMethodsUITestTheme {
                PaymentMethodsUI(
                    paymentMethods = paymentMethods,
                    selectedIndex = paymentMethods.lastIndex,
                    isEnabled = true,
                    onItemSelectedListener = {}
                )
            }
        }
    }

    @Composable
    private fun PaymentMethodsUITestTheme(
        padding: PaddingValues = PaddingValues(vertical = 16.dp),
        content: @Composable () -> Unit
    ) {
        PaymentsTheme(
            shapes = PaymentsTheme.shapesMutable.copy(cornerRadius = testConfig.cornerRadius.value),
            colors = PaymentsTheme.getColors(isDark = testConfig.appearance == Dark)
        ) {
            Surface(color = MaterialTheme.colors.surface) {
                Box(modifier = Modifier.padding(padding)) {
                    content()
                }
            }
        }
    }
}
