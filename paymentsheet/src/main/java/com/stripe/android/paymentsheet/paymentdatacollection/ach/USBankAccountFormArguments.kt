package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.toPaymentMethodIncentive
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.flow.update

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
    val instantDebits: Boolean,
    val linkMode: LinkMode?,
    val incentive: PaymentMethodIncentive?,
    val onBehalfOf: String?,
    val showCheckbox: Boolean,
    val isCompleteFlow: Boolean,
    val isPaymentFlow: Boolean,
    val stripeIntentId: String?,
    val clientSecret: String?,
    val hostedSurface: String,
    val shippingDetails: AddressDetails?,
    val draftPaymentSelection: PaymentSelection?,
    val onMandateTextChanged: (mandate: ResolvableString?, showAbove: Boolean) -> Unit,
    val onLinkedBankAccountChanged: (PaymentSelection.New.USBankAccount?) -> Unit,
    val onUpdatePrimaryButtonUIState: ((PrimaryButton.UIState?) -> (PrimaryButton.UIState?)) -> Unit,
    val onUpdatePrimaryButtonState: (PrimaryButton.State) -> Unit,
    val onError: (ResolvableString?) -> Unit,
) {
    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            hostedSurface: String,
            selectedPaymentMethodCode: String,
            bankFormInteractor: BankFormInteractor,
        ): USBankAccountFormArguments {
            val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
                code = selectedPaymentMethodCode,
                intent = paymentMethodMetadata.stripeIntent,
                paymentMethodSaveConsentBehavior = paymentMethodMetadata.paymentMethodSaveConsentBehavior,
                hasCustomerConfiguration = paymentMethodMetadata.hasCustomerConfiguration,
            )
            val instantDebits = selectedPaymentMethodCode == PaymentMethod.Type.Link.code
            val initializationMode = (viewModel as? PaymentSheetViewModel)
                ?.args
                ?.initializationMode
            val onBehalfOf = (initializationMode as? PaymentElementLoader.InitializationMode.DeferredIntent)
                ?.intentConfiguration
                ?.onBehalfOf
            val stripeIntent = paymentMethodMetadata.stripeIntent
            return USBankAccountFormArguments(
                showCheckbox = isSaveForFutureUseValueChangeable &&
                    // Instant Debits does not support saving for future use
                    instantDebits.not(),
                hostedSurface = hostedSurface,
                instantDebits = instantDebits,
                linkMode = paymentMethodMetadata.linkMode,
                onBehalfOf = onBehalfOf,
                isCompleteFlow = viewModel.isCompleteFlow,
                isPaymentFlow = stripeIntent is PaymentIntent,
                stripeIntentId = stripeIntent.id,
                clientSecret = stripeIntent.clientSecret,
                shippingDetails = viewModel.config.shippingDetails,
                draftPaymentSelection = viewModel.newPaymentSelection?.paymentSelection,
                onMandateTextChanged = viewModel.mandateHandler::updateMandateText,
                onLinkedBankAccountChanged = bankFormInteractor::handleLinkedBankAccountChanged,
                onUpdatePrimaryButtonUIState = { viewModel.customPrimaryButtonUiState.update(it) },
                onUpdatePrimaryButtonState = viewModel::updatePrimaryButtonState,
                onError = viewModel::onError,
                incentive = paymentMethodMetadata.consumerIncentive?.toPaymentMethodIncentive(),
            )
        }
    }
}
