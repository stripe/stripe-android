package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.menu.LinkMenuItem
import com.stripe.android.paymentsheet.R
import com.stripe.android.R as StripeR

internal sealed class WalletPaymentMethodMenuItem(
    override val text: ResolvableString,
    override val isDestructive: Boolean = false
) : LinkMenuItem {
    data class RemoveItem(
        override val text: ResolvableString
    ) : WalletPaymentMethodMenuItem(text, true)

    data object EditCard : WalletPaymentMethodMenuItem(
        text = R.string.stripe_wallet_update_card.resolvableString
    )

    data object SetAsDefault : WalletPaymentMethodMenuItem(
        text = R.string.stripe_wallet_set_as_default.resolvableString
    )

    data object Cancel : WalletPaymentMethodMenuItem(
        text = StripeR.string.stripe_cancel.resolvableString
    )
}
