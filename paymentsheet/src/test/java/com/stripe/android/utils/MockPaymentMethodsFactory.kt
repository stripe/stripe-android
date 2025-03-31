package com.stripe.android.utils

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.ui.core.R

internal object MockPaymentMethodsFactory {

    fun create(): List<SupportedPaymentMethod> {
        return listOf(
            mockPaymentMethod(
                code = "card",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconRequiresTinting = true
            ),
            mockPaymentMethod(
                code = "klarna",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_klarna,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna
            ),
            mockPaymentMethod(
                code = "affirm",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_affirm,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_affirm
            ),
            mockPaymentMethod(
                code = "paypal",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_paypal,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_paypal
            )
        )
    }

    fun mockPaymentMethod(
        code: String,
        displayNameResource: Int,
        iconResource: Int,
        iconRequiresTinting: Boolean = false
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = code,
            displayNameResource = displayNameResource,
            iconResource = iconResource,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            iconRequiresTinting = iconRequiresTinting,
        )
    }

    fun mockPaymentMethod(
        code: String,
        displayName: String,
        iconResource: Int,
        iconRequiresTinting: Boolean = false
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = code,
            displayName = displayName.resolvableString,
            iconResource = iconResource,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            iconRequiresTinting = iconRequiresTinting,
        )
    }
}
