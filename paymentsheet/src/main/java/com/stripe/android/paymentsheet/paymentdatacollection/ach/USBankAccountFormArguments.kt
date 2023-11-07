package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton

/**
 * [USBankAccountFormArguments] provides the arguments required to render the [USBankAccountForm].
 *
 * @param onBehalfOf the connected account of the business of record to attach this US bank account.
 * @param isCompleteFlow whether or not the USBankAccount is being presented in [PaymentSheet].
 * @param isPaymentFlow whether or not the USBankAccount is being used for payment.
 * @param stripeIntentId the [StripeIntent] id.
 * @param clientSecret the client secret.
 * @param shippingDetails the shipping details for this transaction.
 * @param draftPaymentSelection the draft payment information before the customer has confirmed it.
 * @param onMandateTextChanged emitted when the mandate text has been updated, this updated text
 * should be displayed to the user.
 * @param onConfirmUSBankAccount emitted when the confirm button is pressed. The
 * payment method has not been created at this point. This is emitted after going through the ACH
 * flow but before confirming the account with a [StripeIntent]. Use this callback to attach the
 * account to a [StripeIntent].
 * @param onCollectBankAccountResult emitted when the bank account has been collected by the FC SDK.
 * @param onUpdatePrimaryButtonUIState emitted when the [PrimaryButton.UIState] should be updated.
 * The caller should keep track of the current [PrimaryButton.UIState] and update the fields.
 * @param onUpdatePrimaryButtonState emitted when the [PrimaryButton.State] should be updated.
 * @param onError emitted when there is an error
 */
internal class USBankAccountFormArguments(
    val onBehalfOf: String?,
    val isCompleteFlow: Boolean,
    val isPaymentFlow: Boolean,
    val stripeIntentId: String?,
    val clientSecret: String?,
    val shippingDetails: AddressDetails?,
    val draftPaymentSelection: PaymentSelection?,
    val onMandateTextChanged: (mandate: String?, showAbove: Boolean) -> Unit,
    val onConfirmUSBankAccount: (PaymentSelection.New.USBankAccount) -> Unit,
    val onCollectBankAccountResult: ((CollectBankAccountResultInternal) -> Unit)?,
    val onUpdatePrimaryButtonUIState: ((PrimaryButton.UIState?) -> (PrimaryButton.UIState?)) -> Unit,
    val onUpdatePrimaryButtonState: (PrimaryButton.State) -> Unit,
    val onError: (String?) -> Unit,
)
