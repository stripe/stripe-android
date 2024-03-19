package com.stripe.android.lpmfoundations.luxe

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.ui.core.R

object LpmRepositoryTestHelpers {
    val card: SupportedPaymentMethod = cardFromPaymentMethodMetadata() ?: SupportedPaymentMethod(
        code = "card",
        displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
        tintIconOnSelection = true,
        formElements = emptyList(),
    )

    val usBankAccount: SupportedPaymentMethod = SupportedPaymentMethod(
        code = "us_bank_account",
        displayNameResource = R.string.stripe_paymentsheet_payment_method_us_bank_account,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
        tintIconOnSelection = true,
        formElements = emptyList(),
    )

    private fun cardFromPaymentMethodMetadata(): SupportedPaymentMethod? = runCatching {
        PaymentMethodMetadataFactory.create().supportedPaymentMethodForCode(
            code = "card",
            context = ApplicationProvider.getApplicationContext(),
        )
    }.getOrNull()
}
