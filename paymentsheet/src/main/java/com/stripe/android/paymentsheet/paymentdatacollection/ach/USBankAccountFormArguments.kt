package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
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
    val incentive: PaymentMethodIncentive?,
    val linkMode: LinkMode?,
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
    val onFormCompleted: () -> Unit,
    val setAsDefaultPaymentMethodEnabled: Boolean,
    val financialConnectionsAvailability: FinancialConnectionsAvailability?,
    val setAsDefaultMatchesSaveForFutureUse: Boolean,
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
                hasCustomerConfiguration = paymentMethodMetadata.customerMetadata?.hasCustomerConfiguration ?: false,
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
                onFormCompleted = {
                    viewModel.eventReporter.onPaymentMethodFormCompleted(PaymentMethod.Type.USBankAccount.code)
                },
                incentive = paymentMethodMetadata.paymentMethodIncentive,
                setAsDefaultPaymentMethodEnabled =
                paymentMethodMetadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled
                    ?: IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE,
                financialConnectionsAvailability = paymentMethodMetadata.financialConnectionsAvailability,
                setAsDefaultMatchesSaveForFutureUse = viewModel.customerStateHolder.paymentMethods.value.isEmpty(),
            )
        }

        fun create(
            paymentMethodMetadata: PaymentMethodMetadata,
            selectedPaymentMethodCode: String,
            hostedSurface: String,
            setSelection: (PaymentSelection?) -> Unit,
            onMandateTextChanged: (mandate: ResolvableString?, showAbove: Boolean) -> Unit,
            onUpdatePrimaryButtonUIState: ((PrimaryButton.UIState?) -> (PrimaryButton.UIState?)) -> Unit,
            onError: (ResolvableString?) -> Unit,
            onFormCompleted: () -> Unit,
        ): USBankAccountFormArguments {
            val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
                code = selectedPaymentMethodCode,
                intent = paymentMethodMetadata.stripeIntent,
                paymentMethodSaveConsentBehavior = paymentMethodMetadata.paymentMethodSaveConsentBehavior,
                hasCustomerConfiguration = paymentMethodMetadata.customerMetadata?.hasCustomerConfiguration ?: false,
            )
            val instantDebits = selectedPaymentMethodCode == PaymentMethod.Type.Link.code
            val bankFormInteractor = BankFormInteractor(
                updateSelection = setSelection,
                paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
                    paymentMethodMetadata.paymentMethodIncentive
                )
            )
            return USBankAccountFormArguments(
                showCheckbox = isSaveForFutureUseValueChangeable && instantDebits.not(),
                hostedSurface = hostedSurface,
                instantDebits = instantDebits,
                linkMode = paymentMethodMetadata.linkMode,
                onBehalfOf = null,
                isCompleteFlow = false,
                isPaymentFlow = paymentMethodMetadata.stripeIntent is PaymentIntent,
                stripeIntentId = paymentMethodMetadata.stripeIntent.id,
                clientSecret = paymentMethodMetadata.stripeIntent.clientSecret,
                shippingDetails = paymentMethodMetadata.shippingDetails,
                draftPaymentSelection = null,
                onMandateTextChanged = onMandateTextChanged,
                onLinkedBankAccountChanged = bankFormInteractor::handleLinkedBankAccountChanged,
                onUpdatePrimaryButtonUIState = onUpdatePrimaryButtonUIState,
                onUpdatePrimaryButtonState = {
                },
                onError = onError,
                onFormCompleted = onFormCompleted,
                incentive = paymentMethodMetadata.paymentMethodIncentive,
                setAsDefaultPaymentMethodEnabled =
                paymentMethodMetadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled
                    ?: IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE,
                financialConnectionsAvailability = paymentMethodMetadata.financialConnectionsAvailability,
                setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
            )
        }
    }
}
