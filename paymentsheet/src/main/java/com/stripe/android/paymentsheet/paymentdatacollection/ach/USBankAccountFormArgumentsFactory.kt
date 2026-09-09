package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor

internal object USBankAccountFormArgumentsFactory {
    data class IntegrationInputs(
        val isCompleteFlow: Boolean,
        val shippingDetails: AddressDetails?,
        val draftPaymentSelection: PaymentSelection?,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
        val setAsDefaultMatchesSaveForFutureUse: Boolean,
        val termsDisplay: PaymentSheet.TermsDisplay,
        val onAnalyticsEvent: (USBankAccountFormViewModel.AnalyticsEvent) -> Unit,
        val onMandateTextChanged: (mandate: ResolvableString?, showAbove: Boolean) -> Unit,
        val onLinkedBankAccountChanged: (PaymentSelection.New.USBankAccount?) -> Unit,
        val onUpdatePrimaryButtonUIState: ((PrimaryButton.UIState?) -> PrimaryButton.UIState?) -> Unit,
        val onUpdatePrimaryButtonState: (PrimaryButton.State) -> Unit,
        val onError: (ResolvableString?) -> Unit,
        val onFormCompleted: () -> Unit,
    )

    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        selectedPaymentMethodCode: PaymentMethodCode,
        hostedSurface: String,
        integrationInputs: IntegrationInputs,
    ): USBankAccountFormArguments {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = selectedPaymentMethodCode,
            intent = paymentMethodMetadata.stripeIntent,
            paymentMethodSaveConsentBehavior = paymentMethodMetadata.customerMetadata?.saveConsent,
            hasCustomerConfiguration = paymentMethodMetadata.customerMetadata != null,
        )
        val instantDebits = selectedPaymentMethodCode == PaymentMethod.Type.Link.code
        val stripeIntent = paymentMethodMetadata.stripeIntent

        return USBankAccountFormArguments(
            showCheckbox = isSaveForFutureUseValueChangeable && instantDebits.not(),
            hostedSurface = hostedSurface,
            instantDebits = instantDebits,
            linkMode = paymentMethodMetadata.linkMode,
            onBehalfOf = paymentMethodMetadata.onBehalfOf,
            isCompleteFlow = integrationInputs.isCompleteFlow,
            isPaymentFlow = stripeIntent is PaymentIntent,
            stripeIntentId = stripeIntent.id,
            clientSecret = stripeIntent.clientSecret,
            shippingDetails = integrationInputs.shippingDetails,
            draftPaymentSelection = integrationInputs.draftPaymentSelection,
            autocompleteAddressInteractorFactory = integrationInputs.autocompleteAddressInteractorFactory,
            onAnalyticsEvent = integrationInputs.onAnalyticsEvent,
            onMandateTextChanged = integrationInputs.onMandateTextChanged,
            onLinkedBankAccountChanged = integrationInputs.onLinkedBankAccountChanged,
            onUpdatePrimaryButtonUIState = integrationInputs.onUpdatePrimaryButtonUIState,
            onUpdatePrimaryButtonState = integrationInputs.onUpdatePrimaryButtonState,
            onError = integrationInputs.onError,
            onFormCompleted = integrationInputs.onFormCompleted,
            incentive = paymentMethodMetadata.paymentMethodIncentive,
            setAsDefaultPaymentMethodEnabled =
                paymentMethodMetadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled
                    ?: IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE,
            financialConnectionsAvailability = paymentMethodMetadata.financialConnectionsAvailability,
            setAsDefaultMatchesSaveForFutureUse = integrationInputs.setAsDefaultMatchesSaveForFutureUse,
            termsDisplay = integrationInputs.termsDisplay,
            sellerBusinessName = paymentMethodMetadata.sellerBusinessName,
            forceSetupFutureUseBehavior = paymentMethodMetadata.forceSetupFutureUseBehaviorAndNewMandate,
            clientAttributionMetadata = paymentMethodMetadata.clientAttributionMetadata,
        )
    }
}
