package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.lazy.LazyListState
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.definitions.ExternalPaymentMethodUiDefinitionFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.R
import com.stripe.android.utils.MockPaymentMethodsFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class PaymentMethodsUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule()

    @get:Rule
    val paparazziRuleWithStyleChanges = PaparazziRule(
        PaymentSheetAppearance.entries,
        FontSize.entries,
    )

    private val paymentMethods: List<SupportedPaymentMethod> by lazy {
        MockPaymentMethodsFactory.create()
    }

    @Test
    fun testInitialState() {
        paparazziRuleWithStyleChanges.snapshot {
            NewPaymentMethodTabLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                incentive = null,
                onItemSelectedListener = {},
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testLongPaymentMethodName() {
        val bankPaymentMethod = MockPaymentMethodsFactory.mockPaymentMethod(
            code = "us_bank_account",
            displayName = "Greatest US bank account",
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            iconRequiresTinting = true
        )
        val paymentMethods = paymentMethods.toMutableList()
        paymentMethods.add(1, bankPaymentMethod)
        paparazziRuleWithStyleChanges.snapshot {
            NewPaymentMethodTabLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                incentive = null,
                onItemSelectedListener = {},
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testScrolledToEnd() {
        paparazziRule.snapshot {
            NewPaymentMethodTabLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 3,
                isEnabled = true,
                incentive = null,
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
            NewPaymentMethodTabLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                incentive = null,
                onItemSelectedListener = {},
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testExternalPaymentMethod_iconUrlFailsToLoad() {
        val paymentMethods = listOf(
            ExternalPaymentMethodUiDefinitionFactory(
                PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
            ).createSupportedPaymentMethod()
        ).plus(paymentMethods)
        paparazziRule.snapshot {
            NewPaymentMethodTabLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                incentive = null,
                onItemSelectedListener = {},
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testInvalidIconUrlAndInvalidResource() {
        val paymentMethods = listOf(
            SupportedPaymentMethod(
                code = "example_pm",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_affirm,
                iconResource = 0,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                iconRequiresTinting = false,
            )
        ).plus(paymentMethods)
        paparazziRule.snapshot {
            NewPaymentMethodTabLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                incentive = null,
                onItemSelectedListener = {},
                imageLoader = mock(),
            )
        }
    }
}
