package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher.Companion.HOSTED_SURFACE_PAYMENT_ELEMENT
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

internal class EmbeddedFormInteractorFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val paymentMethodCode: PaymentMethodCode,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
) {
    fun create(): DefaultVerticalModeFormInteractor {
        val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            selectionUpdater = {
                embeddedSelectionHolder.set(it)
            }
        )

        val usBankAccountFormArguments = USBankAccountFormArguments.create(
            paymentMethodMetadata = paymentMethodMetadata,
            selectedPaymentMethodCode = paymentMethodCode,
            hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
            setSelection = {
                embeddedSelectionHolder.set(it)
            },
            onMandateTextChanged = { _, _ ->
            },
            onUpdatePrimaryButtonUIState = {
            },
            onError = {
            },
        )

        return DefaultVerticalModeFormInteractor(
            selectedPaymentMethodCode = paymentMethodCode,
            formArguments = formHelper.createFormArguments(paymentMethodCode),
            formElements = formHelper.formElementsForCode(paymentMethodCode),
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            usBankAccountArguments = usBankAccountFormArguments,
            reportFieldInteraction = {
            },
            headerInformation = null,
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            processing = stateFlowOf(false),
            paymentMethodIncentive = PaymentMethodIncentiveInteractor(
                paymentMethodMetadata.paymentMethodIncentive
            ).displayedIncentive,
            coroutineScope = coroutineScope,
        )
    }
}
