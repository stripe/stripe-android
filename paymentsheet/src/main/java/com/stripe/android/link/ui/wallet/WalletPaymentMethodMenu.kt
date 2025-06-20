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
    onSetDefaultClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onUpdateClick: () -> Unit,
) {
    val items = remember(paymentDetails) {
        buildList {
            if (!paymentDetails.isDefault) {
                add(WalletPaymentMethodMenuItem.SetAsDefault)
            }

            if (paymentDetails is ConsumerPaymentDetails.Card) {
                add(WalletPaymentMethodMenuItem.Update)
            }

            add(
                element = WalletPaymentMethodMenuItem.RemoveItem(
                    text = paymentDetails.removeLabel.resolvableString
                )
            )
        }
    }

    LinkMenu(
        modifier = modifier,
        items = items,
        onItemPress = { item ->
            when (item) {
                is WalletPaymentMethodMenuItem.SetAsDefault -> onSetDefaultClick()
                is WalletPaymentMethodMenuItem.RemoveItem -> onRemoveClick()
                is WalletPaymentMethodMenuItem.Update -> onUpdateClick()
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
