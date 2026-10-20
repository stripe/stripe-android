package com.stripe.android.paymentsheet.addresselement

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.SimpleDialogElementUI

@Composable
internal fun AddressElementDismissalDialog(
    onDiscardChanges: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    SimpleDialogElementUI(
        titleText = stringResource(R.string.stripe_paymentsheet_address_element_discard_changes_title),
        messageText = stringResource(R.string.stripe_paymentsheet_address_element_discard_changes_body),
        confirmText = stringResource(R.string.stripe_paymentsheet_address_element_discard_changes_confirm),
        dismissText = stringResource(android.R.string.cancel),
        destructive = true,
        onConfirmListener = onDiscardChanges,
        onDismissListener = onKeepEditing,
    )
}
