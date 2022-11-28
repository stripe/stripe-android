package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.lazy.LazyListState
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.MockPaymentMethodsFactory
import com.stripe.android.utils.screenshots.FontSize2
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance2
import com.stripe.android.utils.screenshots.SystemAppearance2
import org.junit.Rule
import org.junit.Test

class PaymentMethodsUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance2.values(),
        PaymentSheetAppearance2.values(),
        FontSize2.values(),
    )

    private val paymentMethods: List<LpmRepository.SupportedPaymentMethod> by lazy {
        MockPaymentMethodsFactory.create()
    }

    @Test
    fun testInitialState() {
        paparazziRule.snapshot {
            PaymentMethodsUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                onItemSelectedListener = {}
            )
        }
    }

    @Test
    fun testScrolledToEnd() {
        paparazziRule.snapshot {
            PaymentMethodsUI(
                paymentMethods = paymentMethods,
                selectedIndex = 3,
                isEnabled = true,
                state = LazyListState(firstVisibleItemIndex = 3),
                onItemSelectedListener = {}
            )
        }
    }

    @Test
    fun testDisabled() {
        paparazziRule.snapshot {
            PaymentMethodsUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = false,
                onItemSelectedListener = {}
            )
        }
    }
}
