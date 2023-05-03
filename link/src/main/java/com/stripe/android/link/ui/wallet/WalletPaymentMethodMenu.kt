package com.stripe.android.link.ui.wallet

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.stripe.android.link.R
import com.stripe.android.link.model.removeLabel
import com.stripe.android.link.ui.menus.LinkMenu
import com.stripe.android.link.ui.menus.LinkMenuItem
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.R as StripeR

internal sealed class WalletPaymentMethodMenuItem(
    override val textResId: Int,
    override val isDestructive: Boolean = false
) : LinkMenuItem {

    data class RemoveItem(
        @StringRes override val textResId: Int
    ) : WalletPaymentMethodMenuItem(textResId, true)

    object EditCard : WalletPaymentMethodMenuItem(
        textResId = R.string.stripe_wallet_update_card
    )

    object SetAsDefault : WalletPaymentMethodMenuItem(
        textResId = R.string.stripe_wallet_set_as_default
    )

    object Cancel : WalletPaymentMethodMenuItem(
        textResId = StripeR.string.stripe_cancel
    )
}

@Composable
internal fun WalletPaymentMethodMenu(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    onEditClick: () -> Unit,
    onSetDefaultClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val items = buildList {
        if (paymentDetails is ConsumerPaymentDetails.Card) {
            add(WalletPaymentMethodMenuItem.EditCard)
        }

        if (!paymentDetails.isDefault) {
            add(WalletPaymentMethodMenuItem.SetAsDefault)
        }

        add(WalletPaymentMethodMenuItem.RemoveItem(textResId = paymentDetails.removeLabel))
        add(WalletPaymentMethodMenuItem.Cancel)
    }

    LinkMenu(
        items = items,
        onItemPress = { item ->
            when (item) {
                is WalletPaymentMethodMenuItem.EditCard -> onEditClick()
                is WalletPaymentMethodMenuItem.SetAsDefault -> onSetDefaultClick()
                is WalletPaymentMethodMenuItem.RemoveItem -> onRemoveClick()
                is WalletPaymentMethodMenuItem.Cancel -> onCancelClick()
            }
        }
    )
}
