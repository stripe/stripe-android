package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.R as StripeR

@Composable
internal fun RemovePaymentMethodDialogUI(
    paymentMethod: DisplayableSavedPaymentMethod,
    onConfirmListener: () -> Unit,
    onDismissListener: () -> Unit,
) {
    val removeTitle = stringResource(
        R.string.stripe_paymentsheet_remove_pm,
        paymentMethod.displayName,
    )

    SimpleDialogElementUI(
        titleText = removeTitle,
        messageText = paymentMethod.getDescription(LocalContext.current.resources),
        confirmText = stringResource(StripeR.string.stripe_remove),
        dismissText = stringResource(StripeR.string.stripe_cancel),
        destructive = true,
        onConfirmListener = onConfirmListener,
        onDismissListener = onDismissListener,
    )
}
