package com.stripe.android.link.ui.wallet

import androidx.annotation.StringRes
import com.stripe.android.link.ui.menu.LinkMenuItem
import com.stripe.android.paymentsheet.R
import com.stripe.android.R as StripeR

internal sealed class WalletPaymentMethodMenuItem(
    override val textResId: Int,
    override val isDestructive: Boolean = false
) : LinkMenuItem {

    data class RemoveItem(
        @StringRes override val textResId: Int
    ) : WalletPaymentMethodMenuItem(textResId, true)

    data object EditCard : WalletPaymentMethodMenuItem(
        textResId = R.string.stripe_wallet_update_card
    )

    data object SetAsDefault : WalletPaymentMethodMenuItem(
        textResId = R.string.stripe_wallet_set_as_default
    )

    data object Cancel : WalletPaymentMethodMenuItem(
        textResId = StripeR.string.stripe_cancel
    )
}
