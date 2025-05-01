package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.menu.LinkMenuV2
import com.stripe.android.link.ui.menu.MenuPayload
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.R as StripeCoreR

@Composable
internal fun AddPaymentMethodMenu(
    onClose: () -> Unit,
    onAddBankAccount: () -> Unit,
    onAddCard: () -> Unit,
) {
    val payload = remember {
        MenuPayload(
            title = resolvableString(R.string.stripe_add_payment_method),
            items = listOf(
                MenuPayload.MenuItem(
                    title = resolvableString(R.string.stripe_link_add_bank),
                    icon = R.drawable.stripe_link_bank,
                    testTag = null,
                    onClick = onAddBankAccount
                ),
                MenuPayload.MenuItem(
                    title = resolvableString(R.string.stripe_link_add_debit_or_credit_card),
                    icon = StripeCoreR.drawable.stripe_ic_paymentsheet_pm_card,
                    testTag = null,
                    onClick = onAddCard
                )
            )
        )
    }
    LinkMenuV2(
        payload,
        onClose = onClose
    )
}

@Preview(showBackground = true)
@Composable()
fun AddPaymentMethodMenuPreview() {
    Column {
        AddPaymentMethodMenu(
            onClose = {},
            onAddBankAccount = {},
            onAddCard = {}
        )
    }
}
