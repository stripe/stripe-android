package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
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
    val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    val onAnalyticsEvent: (USBankAccountFormViewModel.AnalyticsEvent) -> Unit,
    val onMandateTextChanged: (mandate: ResolvableString?, showAbove: Boolean) -> Unit,
    val onLinkedBankAccountChanged: (PaymentSelection.New.USBankAccount?) -> Unit,
    val onUpdatePrimaryButtonUIState: ((PrimaryButton.UIState?) -> (PrimaryButton.UIState?)) -> Unit,
    val onUpdatePrimaryButtonState: (PrimaryButton.State) -> Unit,
    val onError: (ResolvableString?) -> Unit,
    val onFormCompleted: () -> Unit,
    val setAsDefaultPaymentMethodEnabled: Boolean,
    val financialConnectionsAvailability: FinancialConnectionsAvailability?,
    val setAsDefaultMatchesSaveForFutureUse: Boolean,
    val termsDisplay: PaymentSheet.TermsDisplay,
    val sellerBusinessName: String?,
    val forceSetupFutureUseBehavior: Boolean,
    val clientAttributionMetadata: ClientAttributionMetadata,
) {
    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            hostedSurface: String,
            selectedPaymentMethodCode: String,
            bankFormInteractor: BankFormInteractor,
        ): USBankAccountFormArguments {
            return USBankAccountFormArgumentsFactory.create(
                paymentMethodMetadata = paymentMethodMetadata,
                selectedPaymentMethodCode = selectedPaymentMethodCode,
                hostedSurface = hostedSurface,
                host = USBankAccountFormArgumentsFactory.Host(
                    isCompleteFlow = viewModel.isCompleteFlow,
                    shippingDetails = viewModel.config.shippingDetails,
                    draftPaymentSelection = viewModel.newPaymentSelection?.paymentSelection,
                    autocompleteAddressInteractorFactory = viewModel.autocompleteAddressInteractorFactory,
                    setAsDefaultMatchesSaveForFutureUse =
                        viewModel.customerStateHolder.paymentMethods.value.isEmpty(),
                    termsDisplay = paymentMethodMetadata.termsDisplayForCode(selectedPaymentMethodCode),
                    onAnalyticsEvent = viewModel.eventReporter::onUsBankAccountFormEvent,
                    onMandateTextChanged = viewModel.mandateHandler::updateMandateText,
                    onLinkedBankAccountChanged = bankFormInteractor::handleLinkedBankAccountChanged,
                    onUpdatePrimaryButtonUIState = { viewModel.customPrimaryButtonUiState.update(it) },
                    onUpdatePrimaryButtonState = viewModel::updatePrimaryButtonState,
                    onError = viewModel::onError,
                    onFormCompleted = {
                        viewModel.eventReporter.onPaymentMethodFormCompleted(PaymentMethod.Type.USBankAccount.code)
                    },
                )
            )
        }

        fun createForEmbedded(
            paymentMethodMetadata: PaymentMethodMetadata,
            selectedPaymentMethodCode: String,
            hostedSurface: String,
            isCompleteFlow: Boolean,
            draftPaymentSelection: PaymentSelection?,
            bankFormInteractor: BankFormInteractor,
            hasSavedPaymentMethods: Boolean,
            autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
            onMandateTextChanged: (mandate: ResolvableString?, showAbove: Boolean) -> Unit,
            onAnalyticsEvent: (USBankAccountFormViewModel.AnalyticsEvent) -> Unit,
            onUpdatePrimaryButtonUIState: ((PrimaryButton.UIState?) -> (PrimaryButton.UIState?)) -> Unit,
            onError: (ResolvableString?) -> Unit,
            onFormCompleted: () -> Unit,
        ): USBankAccountFormArguments {
            return USBankAccountFormArgumentsFactory.create(
                paymentMethodMetadata = paymentMethodMetadata,
                selectedPaymentMethodCode = selectedPaymentMethodCode,
                hostedSurface = hostedSurface,
                host = USBankAccountFormArgumentsFactory.Host(
                    isCompleteFlow = isCompleteFlow,
                    shippingDetails = paymentMethodMetadata.shippingDetails,
                    draftPaymentSelection = draftPaymentSelection,
                    autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
                    // If no saved payment methods, then first saved payment method is automatically set as default.
                    setAsDefaultMatchesSaveForFutureUse = !hasSavedPaymentMethods,
                    termsDisplay = paymentMethodMetadata.termsDisplayForType(PaymentMethod.Type.USBankAccount),
                    onAnalyticsEvent = onAnalyticsEvent,
                    onMandateTextChanged = onMandateTextChanged,
                    onLinkedBankAccountChanged = bankFormInteractor::handleLinkedBankAccountChanged,
                    onUpdatePrimaryButtonUIState = onUpdatePrimaryButtonUIState,
                    onUpdatePrimaryButtonState = {},
                    onError = onError,
                    onFormCompleted = onFormCompleted,
                )
            )
        }
    }
}
