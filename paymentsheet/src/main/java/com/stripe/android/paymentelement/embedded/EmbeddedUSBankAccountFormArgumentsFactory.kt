package com.stripe.android.paymentelement.embedded

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher.Companion.HOSTED_SURFACE_PAYMENT_ELEMENT
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor

internal object EmbeddedUSBankAccountFormArgumentsFactory {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        selectedPaymentMethodCode: PaymentMethodCode,
        setSelection: (PaymentSelection?) -> Unit,
        onMandateTextChanged: (mandate: ResolvableString?, showAbove: Boolean) -> Unit,
        onUpdatePrimaryButtonUIState: ((PrimaryButton.UIState?) -> (PrimaryButton.UIState?)) -> Unit,
        onError: (ResolvableString?) -> Unit,
    ): USBankAccountFormArguments {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = selectedPaymentMethodCode,
            intent = paymentMethodMetadata.stripeIntent,
            paymentMethodSaveConsentBehavior = paymentMethodMetadata.paymentMethodSaveConsentBehavior,
            hasCustomerConfiguration = paymentMethodMetadata.hasCustomerConfiguration,
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
            hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
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
            incentive = paymentMethodMetadata.paymentMethodIncentive,
        )
    }
}
