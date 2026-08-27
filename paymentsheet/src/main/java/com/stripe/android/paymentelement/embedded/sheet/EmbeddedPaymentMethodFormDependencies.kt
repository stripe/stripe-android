package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.NewPaymentOptionSelection
import com.stripe.android.paymentsheet.PaymentMethodFormFactory
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class EmbeddedPaymentMethodFormDependencies @Inject constructor(
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
    private val tapToAddHelper: TapToAddHelper,
    private val eventReporter: EventReporter,
    private val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory,
) {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        hasSavedPaymentMethods: Boolean,
    ): PaymentMethodFormFactory.Dependencies {
        return PaymentMethodFormFactory.Dependencies(
            coroutineScope = viewModelScope,
            selection = selectionHolder.selection,
            processing = sheetActivityStateHolder.state.mapAsStateFlow { it.isProcessing },
            validationRequested = sheetActivityStateHolder.validationRequested,
            newPaymentSelectionProvider = ::newPaymentSelection,
            selectionUpdater = selectionHolder::setSelection,
            clearErrorMessages = { sheetActivityStateHolder.updateError(null) },
            reportFieldInteraction = eventReporter::onPaymentMethodFormInteraction,
            eventReporter = eventReporter,
            tapToAddHelper = tapToAddHelper,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            setAsDefaultMatchesSaveForFutureUse = !hasSavedPaymentMethods,
            isCompleteFlow = false,
            shippingDetails = paymentMethodMetadata.shippingDetails,
            draftPaymentSelectionProvider = { null },
            onMandateTextChanged = { mandateText, _ ->
                sheetActivityStateHolder.updateMandate(mandateText)
            },
            onUpdatePrimaryButtonUIState = sheetActivityStateHolder::updatePrimaryButton,
            onUpdatePrimaryButtonState = {},
            onError = sheetActivityStateHolder::updateError,
            termsDisplayProvider = {
                paymentMethodMetadata.termsDisplayForType(PaymentMethod.Type.USBankAccount)
            },
            hasAutomaticallyLaunchedCardScanProvider = { selectedPaymentMethodCode ->
                val paymentSelection = selectionHolder.selection.value as? PaymentSelection.New
                selectedPaymentMethodCode != PaymentMethod.Type.Card.code ||
                    paymentSelection?.paymentMethodCreateParams != null
            },
        )
    }

    private fun newPaymentSelection(code: PaymentMethodCode): NewPaymentOptionSelection? {
        return when (
            val currentSelection = selectionHolder.selection.value
                ?.takeIf { it.paymentMethodType == code }
                ?: selectionHolder.getPreviousNewSelection(code)
        ) {
            is PaymentSelection.ExternalPaymentMethod -> NewPaymentOptionSelection.External(currentSelection)
            is PaymentSelection.CustomPaymentMethod -> NewPaymentOptionSelection.Custom(currentSelection)
            is PaymentSelection.New -> NewPaymentOptionSelection.New(currentSelection)
            else -> null
        }
    }
}
