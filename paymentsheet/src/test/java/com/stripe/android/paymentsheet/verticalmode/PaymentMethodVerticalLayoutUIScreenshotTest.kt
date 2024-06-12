package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.MockPaymentMethodsFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentMethodVerticalLayoutUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(SystemAppearance.LightTheme),
        listOf(PaymentSheetAppearance.DefaultAppearance),
        listOf(FontSize.LargeFont),
    )

    private val paymentMethods: List<SupportedPaymentMethod> by lazy {
        MockPaymentMethodsFactory.create()
    }

    private val savedPaymentMethod: DisplayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard()

    @Test
    fun testSavedAndNewPms() {
        paparazziRule.snapshot {
            PaymentMethodVerticalLayoutUI(
                paymentMethods = paymentMethods,
                displayedSavedPaymentMethod = savedPaymentMethod,
                selectedIndex = 0,
                isEnabled = true,
                onViewMorePaymentMethods = {},
                onItemSelectedListener = {},
                imageLoader = mock(),
            )
        }
    }
}
