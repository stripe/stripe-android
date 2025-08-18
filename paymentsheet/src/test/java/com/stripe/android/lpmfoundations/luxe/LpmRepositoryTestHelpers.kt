package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.ui.core.R

internal object LpmRepositoryTestHelpers {
    val card: SupportedPaymentMethod = cardFromPaymentMethodMetadata() ?: SupportedPaymentMethod(
        code = "card",
        displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
        iconResourceNight = null,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
        iconRequiresTinting = true,
    )

    val usBankAccount: SupportedPaymentMethod = SupportedPaymentMethod(
        code = "us_bank_account",
        displayNameResource = R.string.stripe_paymentsheet_payment_method_us_bank_account,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
        iconResourceNight = null,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
        iconRequiresTinting = true,
    )

    private fun cardFromPaymentMethodMetadata(): SupportedPaymentMethod? = runCatching {
        PaymentMethodMetadataFactory.create().supportedPaymentMethodForCode(
            code = "card",
        )
    }.getOrNull()
}
