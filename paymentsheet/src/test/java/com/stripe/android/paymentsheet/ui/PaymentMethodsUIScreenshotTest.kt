package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.lazy.LazyListState
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.MockPaymentMethodsFactory
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class PaymentMethodsUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        arrayOf(SystemAppearance.LightTheme),
        arrayOf(PaymentSheetAppearance.DefaultAppearance),
        arrayOf(FontSize.LargeFont),
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
                onItemSelectedListener = {},
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testLongPaymentMethodName() {
        val bankPaymentMethod = MockPaymentMethodsFactory.mockPaymentMethod(
            code = "us_bank_account",
            displayNameResource = R.string.stripe_paymentsheet_payment_method_us_bank_account,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            tintIconOnSelection = true
        )
        val paymentMethods = paymentMethods.toMutableList()
        paymentMethods.add(1, bankPaymentMethod)
        paparazziRule.snapshot {
            PaymentMethodsUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                onItemSelectedListener = {},
                imageLoader = mock(),
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
                onItemSelectedListener = {},
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testTwoPaymentMethodsExpandToFit() {
        val paymentMethods = paymentMethods.take(2)
        paparazziRule.snapshot {
            PaymentMethodsUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                onItemSelectedListener = {},
                imageLoader = mock(),
            )
        }
    }
}
