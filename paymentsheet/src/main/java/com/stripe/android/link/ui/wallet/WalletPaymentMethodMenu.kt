package com.stripe.android.link.ui.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.menu.LinkMenu
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.R

@Composable
internal fun WalletPaymentMethodMenu(
    modifier: Modifier = Modifier,
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    onEditClick: () -> Unit,
    onSetDefaultClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val items = remember(paymentDetails) {
        buildList {
//            if (paymentDetails is ConsumerPaymentDetails.Card) {
//                add(WalletPaymentMethodMenuItem.EditCard)
//            }

            if (!paymentDetails.isDefault) {
                add(WalletPaymentMethodMenuItem.SetAsDefault)
            }

            add(
                element = WalletPaymentMethodMenuItem.RemoveItem(
                    text = paymentDetails.removeLabel.resolvableString
                )
            )
            add(WalletPaymentMethodMenuItem.Cancel)
        }
    }

    LinkMenu(
        modifier = modifier,
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

private val ConsumerPaymentDetails.PaymentDetails.removeLabel
    get() = when (this) {
        is ConsumerPaymentDetails.Card,
        is ConsumerPaymentDetails.Passthrough -> R.string.stripe_paymentsheet_remove_card
        is ConsumerPaymentDetails.BankAccount -> R.string.stripe_wallet_remove_linked_account
    }
