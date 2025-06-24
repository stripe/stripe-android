package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.menu.LinkMenuItem
import com.stripe.android.paymentsheet.R

internal sealed class WalletPaymentMethodMenuItem(
    override val text: ResolvableString,
    override val testTag: String,
    override val isDestructive: Boolean = false,
) : LinkMenuItem {
    data class RemoveItem(
        override val text: ResolvableString
    ) : WalletPaymentMethodMenuItem(
        text = text,
        testTag = WALLET_MENU_REMOVE_ITEM_TAG,
        isDestructive = true
    )

    data object SetAsDefault : WalletPaymentMethodMenuItem(
        text = R.string.stripe_wallet_set_as_default.resolvableString,
        testTag = WALLET_MENU_SET_AS_DEFAULT_TAG,
    )

    data object Update : WalletPaymentMethodMenuItem(
        text = R.string.stripe_link_wallet_menu_action_update_card.resolvableString,
        testTag = WALLET_MENU_UPDATE_TAG,
    )
}

internal const val WALLET_MENU_REMOVE_ITEM_TAG = "WALLET_MENU_REMOVE_ITEM_TAG"
internal const val WALLET_MENU_SET_AS_DEFAULT_TAG = "WALLET_MENU_SET_AS_DEFAULT_TAG"
internal const val WALLET_MENU_UPDATE_TAG = "WALLET_MENU_UPDATE_TAG"
