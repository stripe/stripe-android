package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.menu.LinkMenuItem
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.ui.core.R as StripeUiCoreR

internal sealed class AddPaymentMethodOption(
    override val testTag: String,
    override val text: ResolvableString
) : LinkMenuItem {

    data class Bank(
        val financialConnectionsAvailability: FinancialConnectionsAvailability
    ) : AddPaymentMethodOption(
        testTag = "Bank",
        text = resolvableString(StripeUiCoreR.string.stripe_payment_method_bank)
    )

    data object Card : AddPaymentMethodOption(
        testTag = "DebitOrCreditCard",
        text = resolvableString(StripeUiCoreR.string.stripe_payment_method_debit_or_credit_card)
    )

    override val isDestructive: Boolean get() = false
}
