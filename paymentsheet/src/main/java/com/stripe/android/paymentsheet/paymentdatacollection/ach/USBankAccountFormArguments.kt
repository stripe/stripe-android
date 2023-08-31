package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.paymentsheet.model.PaymentSelection

/**
 * [USBankAccountFormArguments] provides the arguments required to render the [USBankAccountForm].
 *
 * @param onMandateTextChanged emitted when the mandate text has been updated, this updated text
 * should be displayed to the user.
 * @param onHandleUSBankAccount emitted when the payment selection has been updated. The
 * payment method has not been created at this point. This is emitted after going through the ACH
 * flow but before confirming the account with a [StripeIntent]. Use this callback to attach the
 * account to a [StripeIntent].
 */
internal class USBankAccountFormArguments(
    val onMandateTextChanged: (String) -> Unit,
    val onHandleUSBankAccount: (PaymentSelection.New.USBankAccount) -> Unit,
)
