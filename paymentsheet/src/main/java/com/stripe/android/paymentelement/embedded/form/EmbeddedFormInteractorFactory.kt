package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher.Companion.HOSTED_SURFACE_PAYMENT_ELEMENT
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class EmbeddedFormInteractorFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val paymentMethodCode: PaymentMethodCode,
    private val hasSavedPaymentMethods: Boolean,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val formActivityStateHelper: FormActivityStateHelper
) {
    fun create(): DefaultVerticalModeFormInteractor {
        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = viewModelScope,
            paymentMethodMetadata = paymentMethodMetadata,
            selectionUpdater = {
                embeddedSelectionHolder.set(it)
            }
        )

        val usBankAccountFormArguments = USBankAccountFormArguments.create(
            paymentMethodMetadata = paymentMethodMetadata,
            selectedPaymentMethodCode = paymentMethodCode,
            hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
            setSelection = embeddedSelectionHolder::set,
            onMandateTextChanged = { mandateText, _ ->
                formActivityStateHelper.updateMandate(mandateText)
            },
            onUpdatePrimaryButtonUIState = formActivityStateHelper::updatePrimaryButton,
            onError = formActivityStateHelper::updateError,
        )

        return DefaultVerticalModeFormInteractor(
            selectedPaymentMethodCode = paymentMethodCode,
            formArguments = formHelper.createFormArguments(paymentMethodCode),
            formElements = formHelper.formElementsForCode(paymentMethodCode),
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            usBankAccountArguments = usBankAccountFormArguments,
            reportFieldInteraction = {
            },
            headerInformation = paymentMethodMetadata.formHeaderInformationForCode(
                code = paymentMethodCode,
                customerHasSavedPaymentMethods = hasSavedPaymentMethods
            ),
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            processing = formActivityStateHelper.state.mapAsStateFlow { it.isProcessing },
            paymentMethodIncentive = PaymentMethodIncentiveInteractor(
                paymentMethodMetadata.paymentMethodIncentive
            ).displayedIncentive,
            coroutineScope = viewModelScope,
        )
    }
}
