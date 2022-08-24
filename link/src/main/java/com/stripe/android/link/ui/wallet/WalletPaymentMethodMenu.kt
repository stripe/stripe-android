package com.stripe.android.link.ui.wallet

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.stripe.android.link.R
import com.stripe.android.link.model.removeLabel
import com.stripe.android.link.ui.menus.LinkMenu
import com.stripe.android.link.ui.menus.LinkMenuItem
import com.stripe.android.model.ConsumerPaymentDetails

internal sealed class WalletPaymentMethodMenuItem(
    override val textResId: Int,
    override val destructive: Boolean = false
) : LinkMenuItem {

    data class RemoveItem(
        @StringRes override val textResId: Int
    ) : WalletPaymentMethodMenuItem(textResId)

    object EditCard : WalletPaymentMethodMenuItem(
        textResId = R.string.wallet_update_card
    )

    object Cancel : WalletPaymentMethodMenuItem(
        textResId = R.string.cancel
    )
}

@Composable
internal fun WalletPaymentMethodMenu(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val items = buildList {
        if (paymentDetails is ConsumerPaymentDetails.Card) {
            add(WalletPaymentMethodMenuItem.EditCard)
        }

        add(WalletPaymentMethodMenuItem.RemoveItem(textResId = paymentDetails.removeLabel))
        add(WalletPaymentMethodMenuItem.Cancel)
    }

    LinkMenu(
        items = items,
        onItemPress = { item ->
            when (item) {
                is WalletPaymentMethodMenuItem.EditCard -> onEditClick()
                is WalletPaymentMethodMenuItem.RemoveItem -> onRemoveClick()
                is WalletPaymentMethodMenuItem.Cancel -> onCancelClick()
            }
        }
    )
}
