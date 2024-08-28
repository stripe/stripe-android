package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    val resources = LocalContext.current.resources

    val removeTitle = stringResource(
        R.string.stripe_paymentsheet_remove_pm,
        paymentMethod.displayName.resolve(),
    )
    val messageText = when (paymentMethod.paymentMethod.type) {
        PaymentMethod.Type.Card -> paymentMethod.getDescription(resources)
        PaymentMethod.Type.USBankAccount -> resources.getString(
            R.string.stripe_remove_bank_account_ending_in,
            paymentMethod.paymentMethod.usBankAccount?.last4
        )
        PaymentMethod.Type.SepaDebit -> resources.getString(
            R.string.stripe_remove_bank_account_ending_in,
            paymentMethod.paymentMethod.sepaDebit?.last4
        )
        else -> ""
    }

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
