package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.definitions.ExternalPaymentMethodUiDefinitionFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.R
import com.stripe.android.utils.MockPaymentMethodsFactory
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

internal class NewPaymentMethodVerticalLayoutUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    private val paymentMethods: List<DisplayablePaymentMethod> by lazy {
        MockPaymentMethodsFactory.create().map { it.asDisplayablePaymentMethod(emptyList()) { } }
    }

    @Test
    fun testInitialStateForVerticalMode() {
        paparazziRule.snapshot {
            NewPaymentMethodVerticalLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testLongPaymentMethodNameForVerticalMode() {
        val bankPaymentMethod = MockPaymentMethodsFactory.mockPaymentMethod(
            code = "us_bank_account",
            displayNameResource = R.string.stripe_paymentsheet_payment_method_us_bank_account,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            iconRequiresTinting = true
        ).asDisplayablePaymentMethod(emptyList()) { }
        val paymentMethods = paymentMethods.toMutableList()
        paymentMethods.add(1, bankPaymentMethod)
        paparazziRule.snapshot {
            NewPaymentMethodVerticalLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testTwoPaymentMethodsExpandToFitForVerticalMode() {
        val paymentMethods = paymentMethods.take(2)
        paparazziRule.snapshot {
            NewPaymentMethodVerticalLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testExternalPaymentMethod_iconUrlFailsToLoadForVerticalMode() {
        val paymentMethods = listOf(
            ExternalPaymentMethodUiDefinitionFactory(
                PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
            ).createSupportedPaymentMethod().asDisplayablePaymentMethod(emptyList()) { }
        ).plus(paymentMethods)
        paparazziRule.snapshot {
            NewPaymentMethodVerticalLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testInvalidIconUrlAndInvalidResourceForVerticalMode() {
        val paymentMethods = listOf(
            DisplayablePaymentMethod(
                code = "example_pm",
                displayName = R.string.stripe_paymentsheet_payment_method_affirm.resolvableString,
                iconResource = 0,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                iconRequiresTinting = false,
                onClick = {},
            )
        ).plus(paymentMethods)
        paparazziRule.snapshot {
            NewPaymentMethodVerticalLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                imageLoader = mock(),
            )
        }
    }
}
