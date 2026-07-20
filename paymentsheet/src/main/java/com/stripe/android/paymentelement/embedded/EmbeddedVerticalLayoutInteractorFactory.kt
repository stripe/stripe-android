package com.stripe.android.paymentelement.embedded

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Builds the [PaymentMethodVerticalLayoutInteractor] shared by the embedded flows. Both the inline
 * embedded content ([com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedContentHelper])
 * and the payment options sheet
 * ([com.stripe.android.paymentelement.embedded.sheet.InitialPaymentOptionsScreenFactory]) wire up the
 * same interactor; only navigation and processing behavior differ, which each call site supplies via
 * [create]'s parameters.
 */
internal class EmbeddedVerticalLayoutInteractorFactory @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val eventReporter: EventReporter,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
) {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        processing: StateFlow<Boolean>,
        temporarySelection: StateFlow<PaymentMethodCode?>,
        walletsState: StateFlow<WalletsState?>,
        walletsStateForAnalytics: () -> WalletsState?,
        formSheetAction: () -> EmbeddedPaymentElement.FormSheetAction?,
        invokeRowSelectionCallback: (() -> Unit)?,
        displaysMandatesInFormScreen: Boolean,
        transitionToManageScreen: () -> Unit,
        transitionToFormScreen: (code: String) -> Unit,
        onUpdatePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    ): PaymentMethodVerticalLayoutInteractor {
        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            // Card scan auto-launch is only relevant in the form, not the list (as the form helper is used here).
            automaticallyLaunchedCardScanFormDataHelper = null,
            tapToAddHelper = null,
            selectionUpdater = {
                selectionHolder.setSelection(it)
                invokeRowSelectionCallback?.invoke()
            },
            // Not important for determining formType so set to default value
            setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
        )

        return DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = processing,
            temporarySelection = temporarySelection,
            selection = selectionHolder.selection,
            paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
                incentive = paymentMethodMetadata.paymentMethodIncentive,
            ),
            formTypeForCode = { code -> formHelper.formTypeForCode(code) },
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            transitionToManageScreen = transitionToManageScreen,
            transitionToFormScreen = transitionToFormScreen,
            paymentMethods = customerStateHolder.paymentMethods,
            mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
            canRemove = customerStateHolder.canRemove,
            canUpdateCardExpiryAndBillingDetails = customerStateHolder.canUpdateCardExpiryAndBillingDetails,
            canChangeCbc = customerStateHolder.canChangeCbc,
            walletsState = walletsState,
            updateSelection = { updatedSelection, _ ->
                selectionHolder.setSelection(updatedSelection)
            },
            isCurrentScreen = stateFlowOf(true),
            reportPaymentMethodTypeSelected = eventReporter::onSelectPaymentMethod,
            reportFormShown = eventReporter::onPaymentMethodFormShown,
            onUpdatePaymentMethod = onUpdatePaymentMethod,
            shouldUpdateVerticalModeSelection = { paymentMethodCode ->
                shouldUpdateVerticalModeSelection(formSheetAction(), formHelper, paymentMethodCode)
            },
            invokeRowSelectionCallback = invokeRowSelectionCallback,
            displaysMandatesInFormScreen = displaysMandatesInFormScreen,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { visiblePaymentMethods, hiddenPaymentMethods ->
                eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                    visiblePaymentMethods = visiblePaymentMethods,
                    hiddenPaymentMethods = hiddenPaymentMethods,
                    walletsState = walletsStateForAnalytics(),
                    isVerticalLayout = true,
                )
            },
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
        )
    }

    private companion object {
        fun shouldUpdateVerticalModeSelection(
            formSheetAction: EmbeddedPaymentElement.FormSheetAction?,
            formHelper: FormHelper,
            paymentMethodCode: String?,
        ): Boolean {
            if (formSheetAction == EmbeddedPaymentElement.FormSheetAction.Confirm) {
                val requiresFormScreen = paymentMethodCode != null &&
                    formHelper.formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
                return !requiresFormScreen
            }
            return true
        }
    }
}
