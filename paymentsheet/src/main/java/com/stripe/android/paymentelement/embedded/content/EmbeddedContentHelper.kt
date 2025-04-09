package com.stripe.android.paymentelement.embedded.content

import android.os.Parcelable
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.mainthread.MainThreadOnlyMutableStateFlow
import com.stripe.android.core.mainthread.MainThreadSavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal interface EmbeddedContentHelper {
    val embeddedContent: StateFlow<EmbeddedContent?>

    fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        rowStyle: Embedded.RowStyle,
        embeddedViewDisplaysMandateText: Boolean,
    )

    fun clearEmbeddedContent()

    fun setSheetLauncher(sheetLauncher: EmbeddedSheetLauncher)

    fun clearSheetLauncher()
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Singleton
internal class DefaultEmbeddedContentHelper @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val savedStateHandle: MainThreadSavedStateHandle,
    private val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext,
    @UIContext private val uiContext: CoroutineContext,
    private val customerRepository: CustomerRepository,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val embeddedWalletsHelper: EmbeddedWalletsHelper,
    private val customerStateHolder: CustomerStateHolder,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    private val confirmationHandler: ConfirmationHandler,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
) : EmbeddedContentHelper {

    private val state: StateFlow<State?> = savedStateHandle.getStateFlow(
        key = STATE_KEY_EMBEDDED_CONTENT,
        initialValue = null
    )

    private val _embeddedContent = MainThreadOnlyMutableStateFlow<EmbeddedContent?>(null)
    override val embeddedContent: StateFlow<EmbeddedContent?> = _embeddedContent.asStateFlow()

    private var sheetLauncher: EmbeddedSheetLauncher? = null

    init {
        coroutineScope.launch {
            state.collect { state ->
                _embeddedContent.value = if (state == null) {
                    null
                } else {
                    EmbeddedContent(
                        interactor = createInteractor(
                            coroutineScope = coroutineScope,
                            paymentMethodMetadata = state.paymentMethodMetadata,
                            walletsState = embeddedWalletsHelper.walletsState(state.paymentMethodMetadata),
                        ),
                        embeddedViewDisplaysMandateText = state.embeddedViewDisplaysMandateText,
                        rowStyle = state.rowStyle,
                    )
                }
            }
        }
    }

    override fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        rowStyle: Embedded.RowStyle,
        embeddedViewDisplaysMandateText: Boolean,
    ) {
        eventReporter.onShowNewPaymentOptions()
        savedStateHandle[STATE_KEY_EMBEDDED_CONTENT] = State(
            paymentMethodMetadata = paymentMethodMetadata,
            rowStyle = rowStyle,
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

    private fun createInteractor(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        walletsState: StateFlow<WalletsState?>,
    ): PaymentMethodVerticalLayoutInteractor {
        val paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
            incentive = paymentMethodMetadata.paymentMethodIncentive,
        )
        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            selectionUpdater = ::setSelection,
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
                confirmationStateHolder.stateFlow.mapAsStateFlow { it != null },
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
                sheetLauncher?.launchManage(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = requireNotNull(customerStateHolder.customer.value),
                    selection = selectionHolder.selection.value,
                )
            },
            transitionToFormScreen = { code ->
                sheetLauncher?.launchForm(
                    code = code,
                    paymentMethodMetadata = paymentMethodMetadata,
                    hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.any {
                        it.type?.code == code
                    },
                    embeddedConfirmationState = confirmationStateHolder.state
                )
            },
            paymentMethods = customerStateHolder.paymentMethods,
            mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
            providePaymentMethodName = savedPaymentMethodMutator.providePaymentMethodName,
            canRemove = customerStateHolder.canRemove,
            onSelectSavedPaymentMethod = ::setSelection,
            walletsState = walletsState,
            canShowWalletsInline = true,
            canShowWalletButtons = false,
            updateSelection = { updatedSelection ->
                setSelection(updatedSelection)
            },
            isCurrentScreen = stateFlowOf(true),
            reportPaymentMethodTypeSelected = eventReporter::onSelectPaymentMethod,
            reportFormShown = eventReporter::onPaymentMethodFormShown,
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
            customerRepository = customerRepository,
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
                )
            },
            isLinkEnabled = stateFlowOf(paymentMethodMetadata.linkState != null),
            isNotPaymentFlow = false,
        )
    }

    private fun setSelection(paymentSelection: PaymentSelection?) {
        selectionHolder.set(paymentSelection)
    }

    @Parcelize
    class State(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val rowStyle: Embedded.RowStyle,
        val embeddedViewDisplaysMandateText: Boolean,
    ) : Parcelable

    companion object {
        const val STATE_KEY_EMBEDDED_CONTENT = "STATE_KEY_EMBEDDED_CONTENT"
    }
}
