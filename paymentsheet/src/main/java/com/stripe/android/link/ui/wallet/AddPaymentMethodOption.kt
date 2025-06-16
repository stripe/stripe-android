package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.menu.LinkMenuItem
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability

internal sealed class AddPaymentMethodOption(
    override val testTag: String,
    override val text: ResolvableString
) : LinkMenuItem {

    data class Bank(
        val financialConnectionsAvailability: FinancialConnectionsAvailability
    ) : AddPaymentMethodOption("Bank", resolvableString("Bank"))

    data object Card : AddPaymentMethodOption("Card", resolvableString("Credit or debit card"))

    override val isDestructive: Boolean get() = false
}
