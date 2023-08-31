package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton

/**
 * [USBankAccountFormArguments] provides the arguments required to render the [USBankAccountForm].
 *
 * @param onMandateTextChanged emitted when the mandate text has been updated, this updated text
 * should be displayed to the user.
 * @param onHandleUSBankAccount emitted when the payment selection has been updated. The
 * payment method has not been created at this point. This is emitted after going through the ACH
 * flow but before confirming the account with a [StripeIntent]. Use this callback to attach the
 * account to a [StripeIntent].
 * @param onUpdatePrimaryButtonUIState emitted when the [PrimaryButton.UIState] should be updated.
 * The caller should keep track of the current [PrimaryButton.UIState] and update the fields.
 */
internal class USBankAccountFormArguments(
    val onMandateTextChanged: (String) -> Unit,
    val onHandleUSBankAccount: (PaymentSelection.New.USBankAccount) -> Unit,
    val onUpdatePrimaryButtonUIState: ((PrimaryButton.UIState?) -> (PrimaryButton.UIState?)) -> Unit,
)
