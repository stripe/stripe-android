package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.R as StripeR

@Composable
internal fun RemovePaymentMethodDialogUI(
    paymentMethod: DisplayableSavedPaymentMethod,
    onConfirmListener: () -> Unit,
    onDismissListener: () -> Unit,
) {
    val removeTitle = paymentMethod.getRemoveDialogTitle().resolve()
    val messageText = paymentMethod.getRemoveDialogDescription().resolve()

    SimpleDialogElementUI(
        titleText = removeTitle,
        messageText = messageText,
        confirmText = stringResource(StripeR.string.stripe_remove),
        dismissText = stringResource(StripeR.string.stripe_cancel),
        destructive = true,
        onConfirmListener = onConfirmListener,
        onDismissListener = onDismissListener,
    )
}

private fun DisplayableSavedPaymentMethod.getRemoveDialogTitle() = when (paymentMethod.type) {
    PaymentMethod.Type.Card -> resolvableString(R.string.stripe_paymentsheet_remove_card_title)
    PaymentMethod.Type.SepaDebit,
    PaymentMethod.Type.USBankAccount ->
        resolvableString(R.string.stripe_paymentsheet_remove_bank_account_question_title)
    else -> resolvableString("")
}

private fun DisplayableSavedPaymentMethod.getRemoveDialogDescription() = when (paymentMethod.type) {
    PaymentMethod.Type.Card -> resolvableString(
        com.stripe.android.R.string.stripe_card_with_last_4,
        this.brandDisplayName(),
        paymentMethod.card?.last4
    )
    PaymentMethod.Type.SepaDebit -> resolvableString(
        R.string.stripe_bank_account_with_last_4,
        paymentMethod.sepaDebit?.last4
    )
    PaymentMethod.Type.USBankAccount -> resolvableString(
        R.string.stripe_bank_account_with_last_4,
        paymentMethod.usBankAccount?.last4
    )
    else -> resolvableString("")
}
