package com.stripe.android.paymentelement.embedded.content

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.DefaultFormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.WalletsState
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
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal interface EmbeddedContentHelper {
    val routes: StateFlow<List<EmbeddedPaymentElement.Route>>

    fun createEmbeddedContentFlow(
        route: EmbeddedPaymentElement.Route,
        useSheets: Boolean,
        onNavigate: (EmbeddedPaymentElement.Route.Type) -> Unit,
        onBack: () -> Unit,
    ): StateFlow<EmbeddedContent?>

    fun goBack()

    fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        rowStyle: Embedded.RowStyle,
        embeddedViewDisplaysMandateText: Boolean,
    )

    fun clearEmbeddedContent()

    fun setCallbackHelper(callbackHelper: EmbeddedResultCallbackHelper)

    fun setSheetLauncher(sheetLauncher: EmbeddedSheetLauncher)

    fun clearSheetLauncher()

    fun clearCallbackHelper()
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Singleton
internal class DefaultEmbeddedContentHelper @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext,
    @UIContext private val uiContext: CoroutineContext,
    private val customerRepository: CustomerRepository,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val sheetStateHolder: SheetStateHolder,
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

    private val _routes = MutableStateFlow<List<EmbeddedPaymentElement.Route>>(emptyList())
    override val routes: StateFlow<List<EmbeddedPaymentElement.Route>> = _routes.asStateFlow()

    private var callbackHelper: EmbeddedResultCallbackHelper? = null
    private var sheetLauncher: EmbeddedSheetLauncher? = null

    init {
        coroutineScope.launch {
            state.collect {
                if (it == null) {
                    _routes.value = emptyList()
                } else if (_routes.value.isEmpty()) {
                    _routes.value = listOf(EmbeddedPaymentElement.Route.PaymentMethods)
                }
            }
        }
    }

    override fun createEmbeddedContentFlow(
        route: EmbeddedPaymentElement.Route,
        useSheets: Boolean,
        onNavigate: (EmbeddedPaymentElement.Route.Type) -> Unit,
        onBack: () -> Unit,
    ): StateFlow<EmbeddedContent?> {
        return state.mapAsStateFlow { currentState ->
            if (currentState == null) {
                return@mapAsStateFlow null
            }

            when (route) {
                is EmbeddedPaymentElement.Route.PaymentMethods -> EmbeddedContent.PaymentMethods(
                    interactor = createLayoutInteractor(
                        coroutineScope = coroutineScope,
                        paymentMethodMetadata = currentState.paymentMethodMetadata,
                        walletsState = embeddedWalletsHelper.walletsState(currentState.paymentMethodMetadata),
                        useSheets = useSheets,
                        onNavigate = onNavigate,
                    ),
                    embeddedViewDisplaysMandateText = currentState.embeddedViewDisplaysMandateText,
                    rowStyle = currentState.rowStyle,
                )
                is EmbeddedPaymentElement.Route.AddPaymentMethod -> {
                    val embeddedConfirmationState = confirmationStateHolder.state ?: throw Exception("LOL")

                    val onClickDelegate = OnClickDelegateOverrideImpl()
                    val stateHelper = DefaultFormActivityStateHelper(
                        paymentMethodMetadata = currentState.paymentMethodMetadata,
                        selectionHolder = selectionHolder,
                        configuration = embeddedConfirmationState.configuration,
                        coroutineScope = coroutineScope,
                        onClickDelegate = onClickDelegate,
                        eventReporter = eventReporter,
                    )

                    EmbeddedContent.AddPaymentMethod(
                        interactor = EmbeddedFormInteractorFactory(
                            paymentMethodMetadata = currentState.paymentMethodMetadata,
                            paymentMethodCode = route.code,
                            embeddedFormHelperFactory = embeddedFormHelperFactory,
                            viewModelScope = coroutineScope,
                            hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.any {
                                it.type?.code == route.code
                            },
                            embeddedSelectionHolder = selectionHolder,
                            formActivityStateHelper = stateHelper,
                            eventReporter = eventReporter
                        ).create(),
                        formActivityStateHelper = stateHelper,
                        onClickDelegate = onClickDelegate,
                        selectionHolder = selectionHolder,
                        paymentMethodMetadata = currentState.paymentMethodMetadata,
                        initializationMode = embeddedConfirmationState.initializationMode,
                        configuration = embeddedConfirmationState.configuration,
                        coroutineScope = coroutineScope,
                        confirmationHandler = confirmationHandler,
                        eventReporter = eventReporter,
                        onResult = { result ->
                            sheetStateHolder.sheetIsOpen = false
                            selectionHolder.setTemporary(null)

                            if (result is FormResult.Complete) {
                                selectionHolder.set(result.selection)

                                if (!useSheets) {
                                    onBack()
                                }

                                if (result.hasBeenConfirmed) {
                                    callbackHelper?.setResult(
                                        EmbeddedPaymentElement.Result.Completed()
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun goBack() {
        _routes.value = _routes.value.run {
            val lastElement = lastOrNull()

            if (lastElement is EmbeddedPaymentElement.Route.AddPaymentMethod) {
                sheetStateHolder.sheetIsOpen = false
                selectionHolder.setTemporary(null)
            }

            dropLast(1)
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

    override fun setCallbackHelper(callbackHelper: EmbeddedResultCallbackHelper) {
        this.callbackHelper = callbackHelper
    }

    override fun setSheetLauncher(sheetLauncher: EmbeddedSheetLauncher) {
        this.sheetLauncher = sheetLauncher
    }

    override fun clearSheetLauncher() {
        sheetLauncher = null
    }

    override fun clearCallbackHelper() {
        callbackHelper = null
    }

    private fun createLayoutInteractor(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        walletsState: StateFlow<WalletsState?>,
        useSheets: Boolean,
        onNavigate: (EmbeddedPaymentElement.Route.Type) -> Unit,
    ): PaymentMethodVerticalLayoutInteractor {
        val paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
            incentive = paymentMethodMetadata.paymentMethodIncentive,
        )
        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            selectionUpdater = ::setSelection,
            // Not important for determining formType so set to default value
            setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
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
                if (sheetStateHolder.sheetIsOpen) return@DefaultPaymentMethodVerticalLayoutInteractor

                sheetStateHolder.sheetIsOpen = true
                selectionHolder.setTemporary(code)

                if (useSheets) {
                    sheetLauncher?.launchForm(
                        code = code,
                        paymentMethodMetadata = paymentMethodMetadata,
                        hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.any {
                            it.type?.code == code
                        },
                        embeddedConfirmationState = confirmationStateHolder.state
                    )
                } else {
                    _routes.value = _routes.value + EmbeddedPaymentElement.Route.AddPaymentMethod(code)
                    onNavigate(EmbeddedPaymentElement.Route.Type.AddPaymentMethod)
                }
            },
            paymentMethods = customerStateHolder.paymentMethods,
            mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
            providePaymentMethodName = savedPaymentMethodMutator.providePaymentMethodName,
            canRemove = customerStateHolder.canRemove,
            canUpdateFullPaymentMethodDetails = customerStateHolder.canUpdateFullPaymentMethodDetails,
            onSelectSavedPaymentMethod = {
                setSelection(PaymentSelection.Saved(it))
            },
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
            shouldUpdateVerticalModeSelection = { paymentMethodCode ->
                val isConfirmFlow = confirmationStateHolder.state?.configuration?.formSheetAction ==
                    EmbeddedPaymentElement.FormSheetAction.Confirm
                if (isConfirmFlow) {
                    val requiresFormScreen = paymentMethodCode != null &&
                        formHelper.formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
                    !requiresFormScreen
                } else {
                    true
                }
            }
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
