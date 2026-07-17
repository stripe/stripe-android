package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.verification.NoOpLinkInlineInteractor
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodRepository
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.DefaultWalletButtonsInteractor
import com.stripe.android.paymentsheet.ui.WalletButtonsContent
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Supplies everything [DefaultEmbeddedContentHelper] renders: the raw [embeddedConfirmationState]
 * (which the wallet buttons and sheet-launch args still read directly) and the derived
 * [embeddedContent] / [walletButtonsContent] built from it. The content-building lives here rather
 * than in the helper so each integration
 * ([EmbeddedPaymentElement][com.stripe.android.paymentelement.EmbeddedPaymentElement] today, checkout
 * later) can supply this one abstraction while the helper stays a thin facade over it.
 */
internal interface EmbeddedContentHelperDataSource {
    val embeddedConfirmationState: StateFlow<EmbeddedConfirmationStateHolder.State?>
    val embeddedContent: StateFlow<EmbeddedContent?>
    val walletButtonsContent: StateFlow<WalletButtonsContent?>
}

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
@Singleton
internal class DefaultEmbeddedContentHelperDataSource @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val eventReporter: EventReporter,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
    @UIContext private val uiContext: CoroutineContext,
    private val savedPaymentMethodRepository: SavedPaymentMethodRepository,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val embeddedLinkHelper: EmbeddedLinkHelper,
    private val rowSelectionImmediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
    private val analyticsCallbackProvider: Provider<AnalyticEventCallback?>,
    private val embeddedWalletsHelper: EmbeddedWalletsHelper,
    private val customerStateHolder: CustomerStateHolder,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    private val confirmationHandler: ConfirmationHandler,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val linkAccountHolder: LinkAccountHolder,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
    private val sheetLauncherHolder: EmbeddedSheetLauncherHolder,
) : EmbeddedContentHelperDataSource {

    override val embeddedConfirmationState: StateFlow<EmbeddedConfirmationStateHolder.State?> =
        confirmationStateHolder.stateFlow

    private val state: StateFlow<EmbeddedContentState?> =
        embeddedConfirmationState.mapAsStateFlow { it?.toEmbeddedContentState() }

    private val _embeddedContent = MutableStateFlow<EmbeddedContent?>(null)
    override val embeddedContent: StateFlow<EmbeddedContent?> = _embeddedContent.asStateFlow()

    private val _walletButtonsContent = MutableStateFlow<WalletButtonsContent?>(null)
    override val walletButtonsContent: StateFlow<WalletButtonsContent?> = _walletButtonsContent.asStateFlow()

    init {
        coroutineScope.launch {
            state.collect { state ->
                _embeddedContent.value = if (state == null) {
                    null
                } else {
                    val isImmediateAction = internalRowSelectionCallback.get() != null
                    EmbeddedContent(
                        interactor = createInteractor(
                            coroutineScope = coroutineScope,
                            paymentMethodMetadata = state.paymentMethodMetadata,
                            walletsState = embeddedWalletsHelper.walletsState(state.paymentMethodMetadata),
                            isImmediateAction = isImmediateAction,
                            embeddedViewDisplaysMandateText = state.embeddedViewDisplaysMandateText,
                        ),
                        embeddedViewDisplaysMandateText = state.embeddedViewDisplaysMandateText,
                        appearance = state.appearance,
                        isImmediateAction = isImmediateAction,
                    )
                }
            }
        }

        coroutineScope.launch {
            state.collect { state ->
                _walletButtonsContent.value = if (state == null) {
                    null
                } else {
                    WalletButtonsContent(
                        interactor = createWalletButtonsInteractor(
                            coroutineScope = coroutineScope,
                        ),
                    )
                }
            }
        }
    }

    private fun createWalletButtonsInteractor(
        coroutineScope: CoroutineScope,
    ): WalletButtonsInteractor {
        return DefaultWalletButtonsInteractor.create(
            embeddedLinkHelper = embeddedLinkHelper,
            contentHelperDataSource = this,
            confirmationHandler = confirmationHandler,
            coroutineScope = coroutineScope,
            errorReporter = errorReporter,
            eventReporter = eventReporter,
            linkPaymentLauncher = linkPaymentLauncher,
            linkAccountHolder = linkAccountHolder,
            linkInlineInteractor = NoOpLinkInlineInteractor(),
            analyticsCallbackProvider = analyticsCallbackProvider,
        )
    }

    private fun createInteractor(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        walletsState: StateFlow<WalletsState?>,
        isImmediateAction: Boolean,
        embeddedViewDisplaysMandateText: Boolean,
    ): PaymentMethodVerticalLayoutInteractor {
        val paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
            incentive = paymentMethodMetadata.paymentMethodIncentive,
        )
        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            // Card scan auto-launch is only relevant in the form, not the list (as the form helper is used in here).
            automaticallyLaunchedCardScanFormDataHelper = null,
            tapToAddHelper = null,
            selectionUpdater = {
                setSelection(it)
                invokeRowSelectionCallback()
            },
            // Not important for determining formType so set to default value
            setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper
        )
        val savedPaymentMethodMutator = createSavedPaymentMethodMutator(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
        )

        return DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = combineAsStateFlow(
                confirmationHandler.state.mapAsStateFlow { it is ConfirmationHandler.State.Confirming },
                embeddedConfirmationState.mapAsStateFlow { it != null },
            ) { confirmationStateValid, configurationStateValid ->
                confirmationStateValid && configurationStateValid
            },
            temporarySelection = selectionHolder.temporarySelection,
            selection = selectionHolder.selection,
            paymentMethodIncentiveInteractor = paymentMethodIncentiveInteractor,
            formTypeForCode = { code ->
                formHelper.formTypeForCode(code)
            },
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            transitionToManageScreen = {
                sheetLauncherHolder.sheetLauncher?.launchManage(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = requireNotNull(customerStateHolder.customer.value),
                    selection = selectionHolder.selection.value,
                    embeddedConfirmationState = embeddedConfirmationState.value,
                )
            },
            transitionToFormScreen = { code ->
                sheetLauncherHolder.sheetLauncher?.launchForm(
                    code = code,
                    paymentMethodMetadata = paymentMethodMetadata,
                    embeddedConfirmationState = embeddedConfirmationState.value,
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
            updateSelection = { updatedSelection, requiresConfirmation ->
                setSelection(updatedSelection)
            },
            isCurrentScreen = stateFlowOf(true),
            reportPaymentMethodTypeSelected = eventReporter::onSelectPaymentMethod,
            reportFormShown = eventReporter::onPaymentMethodFormShown,
            onUpdatePaymentMethod = savedPaymentMethodMutator::updatePaymentMethod,
            shouldUpdateVerticalModeSelection = { paymentMethodCode ->
                val isConfirmFlow = embeddedConfirmationState.value
                    ?.configuration?.formSheetAction == EmbeddedPaymentElement.FormSheetAction.Confirm
                if (isConfirmFlow) {
                    val requiresFormScreen = paymentMethodCode != null &&
                        formHelper.formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
                    !requiresFormScreen
                } else {
                    true
                }
            },
            invokeRowSelectionCallback = ::invokeRowSelectionCallback,
            displaysMandatesInFormScreen = isImmediateAction && embeddedViewDisplaysMandateText,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { visiblePaymentMethods, hiddenPaymentMethods ->
                eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                    visiblePaymentMethods = visiblePaymentMethods,
                    hiddenPaymentMethods = hiddenPaymentMethods,
                    walletsState = walletsState.value,
                    isVerticalLayout = true,
                )
            },
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper
        )
    }

    private fun createSavedPaymentMethodMutator(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerStateHolder: CustomerStateHolder,
    ): SavedPaymentMethodMutator {
        return SavedPaymentMethodMutator(
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            eventReporter = eventReporter,
            coroutineScope = coroutineScope,
            workContext = workContext,
            uiContext = uiContext,
            savedPaymentMethodRepository = savedPaymentMethodRepository,
            selection = selectionHolder.selection,
            setSelection = ::setSelection,
            customerStateHolder = customerStateHolder,
            prePaymentMethodRemoveActions = {},
            postPaymentMethodRemoveActions = {},
            onUpdatePaymentMethod = { _, _, _, _, _ ->
                sheetLauncherHolder.sheetLauncher?.launchManage(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = requireNotNull(customerStateHolder.customer.value),
                    selection = selectionHolder.selection.value,
                    embeddedConfirmationState = embeddedConfirmationState.value,
                )
            },
            isLinkEnabled = stateFlowOf(paymentMethodMetadata.linkState != null),
            isNotPaymentFlow = false,
            accountLinkBrandFlow = linkAccountHolder.linkAccountInfo.mapAsStateFlow { it.account?.linkBrand },
        )
    }

    private fun invokeRowSelectionCallback() {
        rowSelectionImmediateActionHandler.invoke()
    }

    private fun setSelection(paymentSelection: PaymentSelection?) {
        selectionHolder.setSelection(paymentSelection)
    }
}

private fun EmbeddedConfirmationStateHolder.State.toEmbeddedContentState(): EmbeddedContentState {
    return EmbeddedContentState(
        paymentMethodMetadata = paymentMethodMetadata,
        appearance = configuration.appearance.embeddedAppearance,
        embeddedViewDisplaysMandateText = configuration.embeddedViewDisplaysMandateText,
    )
}
