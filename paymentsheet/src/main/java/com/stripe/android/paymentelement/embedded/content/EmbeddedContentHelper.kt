package com.stripe.android.paymentelement.embedded.content

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.verification.NoOpLinkInlineInteractor
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedVerticalLayoutInteractorFactory
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodRepository
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.DefaultWalletButtonsInteractor
import com.stripe.android.paymentsheet.ui.WalletButtonsContent
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal interface EmbeddedContentHelper {
    val embeddedContent: StateFlow<EmbeddedContent?>
    val walletButtonsContent: StateFlow<WalletButtonsContent?>

    fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        appearance: Embedded,
        embeddedViewDisplaysMandateText: Boolean,
    )

    fun clearEmbeddedContent()

    fun setSheetLauncher(sheetLauncher: EmbeddedSheetLauncher)

    fun clearSheetLauncher()

    fun presentPaymentOptions()
}

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
@Singleton
internal class DefaultEmbeddedContentHelper @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
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
    private val embeddedVerticalLayoutInteractorFactory: EmbeddedVerticalLayoutInteractorFactory,
    private val confirmationHandler: ConfirmationHandler,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val linkAccountHolder: LinkAccountHolder,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper
) : EmbeddedContentHelper {

    private val state: StateFlow<State?> = savedStateHandle.getStateFlow(
        key = STATE_KEY_EMBEDDED_CONTENT,
        initialValue = null
    )

    private val _embeddedContent = MutableStateFlow<EmbeddedContent?>(null)
    override val embeddedContent: StateFlow<EmbeddedContent?> = _embeddedContent.asStateFlow()

    private val _walletButtonsContent = MutableStateFlow<WalletButtonsContent?>(null)
    override val walletButtonsContent: StateFlow<WalletButtonsContent?> = _walletButtonsContent.asStateFlow()

    private var sheetLauncher: EmbeddedSheetLauncher? = null

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

    override fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        appearance: Embedded,
        embeddedViewDisplaysMandateText: Boolean,
    ) {
        eventReporter.onShowNewPaymentOptions()
        savedStateHandle[STATE_KEY_EMBEDDED_CONTENT] = State(
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
        )
    }

    override fun clearEmbeddedContent() {
        savedStateHandle[STATE_KEY_EMBEDDED_CONTENT] = null
    }

    override fun setSheetLauncher(sheetLauncher: EmbeddedSheetLauncher) {
        this.sheetLauncher = sheetLauncher
    }

    override fun clearSheetLauncher() {
        sheetLauncher = null
    }

    override fun presentPaymentOptions() {
        val state = state.value
        if (state == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_PRESENT_PAYMENT_OPTIONS_NOT_CONFIGURED
            )
            return
        }
        val launcher = sheetLauncher
        if (launcher == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_PRESENT_PAYMENT_OPTIONS_NO_LAUNCHER
            )
            return
        }
        launcher.launchPaymentOptions(
            paymentMethodMetadata = state.paymentMethodMetadata,
            customerState = customerStateHolder.customer.value,
            selection = selectionHolder.selection.value,
            embeddedConfirmationState = confirmationStateHolder.state,
        )
    }

    private fun createWalletButtonsInteractor(
        coroutineScope: CoroutineScope,
    ): WalletButtonsInteractor {
        return DefaultWalletButtonsInteractor.create(
            embeddedLinkHelper = embeddedLinkHelper,
            confirmationStateHolder = confirmationStateHolder,
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
        val savedPaymentMethodMutator = createSavedPaymentMethodMutator(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
        )

        return embeddedVerticalLayoutInteractorFactory.create(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = combineAsStateFlow(
                confirmationHandler.state.mapAsStateFlow { it is ConfirmationHandler.State.Confirming },
                confirmationStateHolder.stateFlow.mapAsStateFlow { it != null },
            ) { confirmationStateValid, configurationStateValid ->
                confirmationStateValid && configurationStateValid
            },
            temporarySelection = selectionHolder.temporarySelection,
            walletsState = walletsState,
            walletsStateForAnalytics = { walletsState.value },
            formSheetAction = { confirmationStateHolder.state?.configuration?.formSheetAction },
            invokeRowSelectionCallback = ::invokeRowSelectionCallback,
            displaysMandatesInFormScreen = isImmediateAction && embeddedViewDisplaysMandateText,
            transitionToManageScreen = {
                sheetLauncher?.launchManage(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = requireNotNull(customerStateHolder.customer.value),
                    selection = selectionHolder.selection.value,
                    embeddedConfirmationState = confirmationStateHolder.state,
                )
            },
            transitionToFormScreen = { code ->
                sheetLauncher?.launchForm(
                    code = code,
                    paymentMethodMetadata = paymentMethodMetadata,
                    embeddedConfirmationState = confirmationStateHolder.state,
                    customerState = customerStateHolder.customer.value,
                    promotion = paymentMethodMessagePromotionsHelper.getPromotionIfAvailableForCode(
                        code = code,
                        metadata = paymentMethodMetadata
                    )
                )
            },
            onUpdatePaymentMethod = savedPaymentMethodMutator::updatePaymentMethod,
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
                sheetLauncher?.launchManage(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = requireNotNull(customerStateHolder.customer.value),
                    selection = selectionHolder.selection.value,
                    embeddedConfirmationState = confirmationStateHolder.state,
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

    @Parcelize
    class State(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val appearance: Embedded,
        val embeddedViewDisplaysMandateText: Boolean,
    ) : Parcelable

    companion object {
        const val STATE_KEY_EMBEDDED_CONTENT = "STATE_KEY_EMBEDDED_CONTENT"
    }
}
