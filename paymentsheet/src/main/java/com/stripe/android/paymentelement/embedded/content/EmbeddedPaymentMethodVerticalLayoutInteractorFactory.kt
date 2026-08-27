package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.NewPaymentOptionSelection
import com.stripe.android.paymentsheet.PaymentMethodFormFactory
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal fun interface EmbeddedPaymentMethodVerticalLayoutInteractorFactory {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        configuration: EmbeddedPaymentElement.Configuration,
        walletsState: StateFlow<WalletsState?>,
        isImmediateAction: Boolean,
        embeddedViewDisplaysMandateText: Boolean,
    ): PaymentMethodVerticalLayoutInteractor
}

internal class DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory @Inject constructor(
    private val eventReporter: EventReporter,
    private val paymentMethodFormFactory: PaymentMethodFormFactory,
    private val confirmationHandler: ConfirmationHandler,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
    private val rowSelectionImmediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val sheetStateHolder: SheetStateHolder,
    private val savedPaymentMethodMutatorFactory: EmbeddedContentSavedPaymentMethodMutatorFactory,
    private val linkAccountHolder: LinkAccountHolder,
) : EmbeddedPaymentMethodVerticalLayoutInteractorFactory {

    @Suppress("LongMethod")
    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        configuration: EmbeddedPaymentElement.Configuration,
        walletsState: StateFlow<WalletsState?>,
        isImmediateAction: Boolean,
        embeddedViewDisplaysMandateText: Boolean,
    ): PaymentMethodVerticalLayoutInteractor {
        val interactorScope = coroutineScope.childScope(Dispatchers.Default)
        val formHelperScope = interactorScope.childScope(Dispatchers.Main)
        val paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
            incentive = paymentMethodMetadata.paymentMethodIncentive,
        )
        val formHelper = paymentMethodFormFactory.createFormHelper(
            PaymentMethodFormFactory.FormHelperArguments(
                coroutineScope = formHelperScope,
                linkInlineHandler = com.stripe.android.paymentsheet.LinkInlineHandler.create(),
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = ::newPaymentSelection,
                selectionUpdater = {
                    selectionHolder.setSelection(it)
                    rowSelectionImmediateActionHandler.invoke()
                },
                eventReporter = eventReporter,
                setAsDefaultMatchesSaveForFutureUse =
                    FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
                automaticallyLaunchedCardScanFormDataHelper = null,
                tapToAddHelper = null,
                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                autocompleteAddressInteractorFactory = null,
            )
        )
        val savedPaymentMethodMutator = savedPaymentMethodMutatorFactory.create(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = configuration,
        )

        return DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = confirmationHandler.state.mapAsStateFlow { it is ConfirmationHandler.State.Confirming },
            temporarySelection = selectionHolder.temporarySelection,
            selection = selectionHolder.selection,
            paymentMethodIncentiveInteractor = paymentMethodIncentiveInteractor,
            formTypeForCode = { code ->
                formHelper.formTypeForCode(code)
            },
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            transitionToManageScreen = {
                sheetStateHolder.sheetLauncher?.launchManage(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = requireNotNull(customerStateHolder.customer.value),
                    selection = selectionHolder.selection.value,
                    configuration = configuration,
                )
            },
            transitionToFormScreen = { code ->
                sheetStateHolder.sheetLauncher?.launchForm(
                    code = code,
                    paymentMethodMetadata = paymentMethodMetadata,
                    configuration = configuration,
                    customerState = customerStateHolder.customer.value,
                    promotion = paymentMethodMessagePromotionsHelper.getPromotionIfAvailableForCode(
                        code = code,
                        metadata = paymentMethodMetadata
                    )
                )
            },
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
            onUpdatePaymentMethod = savedPaymentMethodMutator::updatePaymentMethod,
            shouldUpdateVerticalModeSelection = { paymentMethodCode ->
                val isConfirmFlow = configuration.formSheetAction ==
                    EmbeddedPaymentElement.FormSheetAction.Confirm
                if (isConfirmFlow) {
                    val requiresFormScreen = paymentMethodCode != null &&
                        formHelper.formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
                    !requiresFormScreen
                } else {
                    true
                }
            },
            invokeRowSelectionCallback = rowSelectionImmediateActionHandler::invoke,
            displaysMandatesInFormScreen = isImmediateAction && embeddedViewDisplaysMandateText,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { visiblePaymentMethods, hiddenPaymentMethods ->
                eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                    visiblePaymentMethods = visiblePaymentMethods,
                    hiddenPaymentMethods = hiddenPaymentMethods,
                    walletsState = walletsState.value,
                    isVerticalLayout = true,
                )
            },
            // Embedded renders mandate text through its own path, not the mandate-above-button handler.
            updateMandateText = null,
            linkAccount = linkAccountHolder.linkAccountInfo,
            coroutineScope = interactorScope,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
        )
    }

    private fun newPaymentSelection(code: String): NewPaymentOptionSelection? {
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
