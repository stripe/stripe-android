package com.stripe.android.utils

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.paymentsheet.verticalmode.DisplayablePaymentMethod
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

    fun createDisplayablePaymentMethods(): List<DisplayablePaymentMethod> {
        return listOf(
            mockDisplayablePaymentMethod(
                code = "card",
                subtitle = null,
                displayName = "Card".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
            ),
            mockDisplayablePaymentMethod(
                code = "us_bank_account",
                subtitle = null,
                displayName = "US bank account".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            ),
            mockDisplayablePaymentMethod(
                code = "klarna",
                subtitle = null,
                displayName = "Klarna".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna
            ),
            mockDisplayablePaymentMethod(
                code = "affirm",
                subtitle = null,
                displayName = "Affirm".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_affirm
            ),
        )
    }

    fun createDisplayablePaymentMethodsWithSelectedCard(): List<DisplayablePaymentMethod> {
        return listOf(
            mockDisplayablePaymentMethod(
                code = "card",
                subtitle = "Visa *** 4242".resolvableString,
                displayName = "Card".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
            ),
            mockDisplayablePaymentMethod(
                code = "us_bank_account",
                subtitle = null,
                displayName = "US bank account".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            ),
            mockDisplayablePaymentMethod(
                code = "klarna",
                subtitle = null,
                displayName = "Klarna".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna
            ),
            mockDisplayablePaymentMethod(
                code = "affirm",
                subtitle = null,
                displayName = "Affirm".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_affirm
            ),
        )
    }

    fun createDisplayablePaymentMethodsWithSelectedUSBankAccount(): List<DisplayablePaymentMethod> {
        return listOf(
            mockDisplayablePaymentMethod(
                code = "card",
                subtitle = null,
                displayName = "Card".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
            ),
            mockDisplayablePaymentMethod(
                code = "us_bank_account",
                subtitle = "**** 6789".resolvableString,
                displayName = "US bank Account".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            ),
            mockDisplayablePaymentMethod(
                code = "klarna",
                subtitle = null,
                displayName = "Klarna".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna
            ),
            mockDisplayablePaymentMethod(
                code = "affirm",
                subtitle = null,
                displayName = "Affirm".resolvableString,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_affirm
            ),
        )
    }

    fun mockDisplayablePaymentMethod(
        code: String,
        displayName: ResolvableString,
        iconResource: Int,
        subtitle: ResolvableString?,
    ): DisplayablePaymentMethod {
        return DisplayablePaymentMethod(
            code = code,
            subtitle = subtitle,
            displayName = displayName,
            iconResource = iconResource,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            iconRequiresTinting = false,
            promoBadge = null,
            onClick = {}
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
