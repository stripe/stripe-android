package com.stripe.android.link.ui.wallet

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.link.R
import com.stripe.android.link.ui.menus.LinkMenu
import com.stripe.android.link.ui.menus.LinkMenuItem
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.R as StripeR

@Composable
internal fun WalletPaymentMethodMenu(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    onEditClick: () -> Unit,
    onSetDefaultClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val items = remember {
        buildList {
            if (paymentDetails is ConsumerPaymentDetails.Card) {
                add(WalletPaymentMethodMenuItem.EditCard)
            }

            if (!paymentDetails.isDefault) {
                add(WalletPaymentMethodMenuItem.SetAsDefault)
            }

            add(WalletPaymentMethodMenuItem.RemoveItem(textResId = paymentDetails.removeLabel))
            add(WalletPaymentMethodMenuItem.Cancel)
        }
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

internal val ConsumerPaymentDetails.PaymentDetails.removeLabel
    get() = when (this) {
        is ConsumerPaymentDetails.Card,
        is ConsumerPaymentDetails.Passthrough -> R.string.stripe_wallet_remove_card
        is ConsumerPaymentDetails.BankAccount -> R.string.stripe_wallet_remove_linked_account
    }
