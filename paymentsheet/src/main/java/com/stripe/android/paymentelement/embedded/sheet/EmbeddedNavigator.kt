package com.stripe.android.paymentelement.embedded.sheet

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.form.FormActivityError
import com.stripe.android.paymentelement.embedded.form.FormActivityPrimaryButton
import com.stripe.android.paymentelement.embedded.form.FormScreenContent
import com.stripe.android.paymentelement.embedded.form.USBankAccountMandate
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.CvcRecollectionField
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodTabLayoutUI
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodUI
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.utils.addPaymentMethodTitle
import com.stripe.android.paymentsheet.utils.isOnlyOneNonCardPaymentMethod
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutUI
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.stripeFormInsets
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.Closeable
import javax.inject.Inject

internal class EmbeddedNavigator private constructor(
    private val eventReporter: EventReporter,
    private val navigationHandler: NavigationHandler<Screen>
) {
    constructor(
        coroutineScope: CoroutineScope,
        initialScreen: Screen,
        eventReporter: EventReporter,
    ) : this(
        coroutineScope = coroutineScope,
        initialBackStack = listOf(initialScreen),
        eventReporter = eventReporter,
    )

    constructor(
        coroutineScope: CoroutineScope,
        initialBackStack: List<Screen>,
        eventReporter: EventReporter,
    ) : this(
        eventReporter = eventReporter,
        navigationHandler = NavigationHandler(
            coroutineScope = coroutineScope,
            initialBackStack = initialBackStack,
            shouldRemoveInitialScreenOnTransition = false,
            poppedScreenHandler = {},
        )
    )

    val screen: StateFlow<Screen> = navigationHandler.currentScreen
    val canGoBack: Boolean
        get() = navigationHandler.canGoBack

    // result value is shouldInvokeRowSelectionCallback
    private val _result = MutableSharedFlow<Boolean?>(replay = 1)
    val result: SharedFlow<Boolean?> = _result.asSharedFlow()

    init {
        onScreenShown(screen.value)
    }

    fun performAction(action: Action) {
        when (action) {
            is Action.Back -> {
                onScreenHidden(screen.value)
                if (navigationHandler.canGoBack) {
                    navigationHandler.pop()
                } else {
                    _result.tryEmit(null)
                }
            }
            is Action.Close -> {
                onScreenHidden(screen.value)
                _result.tryEmit(action.shouldInvokeRowSelectionCallback)
            }
            is Action.GoToScreen -> {
                navigationHandler.transitionToWithDelay(action.screen)
                onScreenShown(action.screen)
            }
        }
    }

    private fun onScreenShown(screen: Screen) {
        when (screen) {
            is Screen.ManageAll -> eventReporter.onShowManageSavedPaymentMethods()
            is Screen.ManageUpdate -> eventReporter.onShowEditablePaymentOption()
            is Screen.Form -> Unit
            is Screen.VerticalPaymentOptions -> eventReporter.onShowNewPaymentOptions()
            is Screen.HorizontalPaymentOptions -> eventReporter.onShowNewPaymentOptions()
            is Screen.HorizontalSavedPaymentOptions -> eventReporter.onShowExistingPaymentOptions()
        }
    }

    private fun onScreenHidden(screen: Screen) {
        when (screen) {
            is Screen.ManageAll -> Unit
            is Screen.ManageUpdate -> eventReporter.onHideEditablePaymentOption()
            is Screen.Form -> Unit
            is Screen.VerticalPaymentOptions -> Unit
            is Screen.HorizontalPaymentOptions -> Unit
            is Screen.HorizontalSavedPaymentOptions -> Unit
        }
    }

    sealed class Screen {
        @Composable
        abstract fun Content()

        abstract fun topBarState(): StateFlow<PaymentSheetTopBarState?>

        abstract fun title(): StateFlow<ResolvableString?>

        abstract fun isPerformingNetworkOperation(): StateFlow<Boolean>

        class ManageAll(
            private val interactor: ManageScreenInteractor,
        ) : Screen(), Closeable {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
                return interactor.state.mapAsStateFlow { state ->
                    state.topBarState(interactor)
                }
            }

            override fun title(): StateFlow<ResolvableString?> {
                return interactor.state.mapAsStateFlow { state ->
                    state.title
                }
            }

            override fun isPerformingNetworkOperation(): StateFlow<Boolean> {
                return stateFlowOf(false)
            }

            @Composable
            override fun Content() {
                ManageScreenUI(interactor = interactor)
                PaymentSheetContentPadding(subtractingExtraPadding = 12.dp)
            }

            override fun close() {
                interactor.close()
            }
        }

        class ManageUpdate(
            private val interactor: UpdatePaymentMethodInteractor,
        ) : Screen(), Closeable {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(interactor.topBarState)

            override fun title(): StateFlow<ResolvableString?> {
                return stateFlowOf(interactor.screenTitle)
            }

            override fun isPerformingNetworkOperation(): StateFlow<Boolean> {
                return interactor.state.mapAsStateFlow { it.status.isPerformingNetworkOperation }
            }

            @Composable
            override fun Content() {
                UpdatePaymentMethodUI(interactor = interactor, modifier = Modifier.Companion)
                PaymentSheetContentPadding(subtractingExtraPadding = 16.dp)
            }

            override fun close() {
                interactor.close()
            }
        }

        class Form(
            val formInteractor: VerticalModeFormInteractor,
            private val eventReporter: EventReporter,
            private val sheetActivityStateHolder: SheetActivityStateHolder,
            private val confirmationHelper: SheetActivityConfirmationHelper,
            private val embeddedSelectionHolder: EmbeddedSelectionHolder,
            private val savedPaymentMethodConfirmInteractorFactory: SavedPaymentMethodConfirmInteractor.Factory,
            private val customerStateHolder: CustomerStateHolder,
            private val launchMode: EmbeddedLaunchMode,
        ) : Screen(), Closeable {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    isLiveMode = formInteractor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )

            override fun title(): StateFlow<ResolvableString?> = stateFlowOf(null)

            override fun isPerformingNetworkOperation(): StateFlow<Boolean> {
                return sheetActivityStateHolder.state.mapAsStateFlow { it.isProcessing }
            }

            @Composable
            override fun Content() {
                val state by sheetActivityStateHolder.state.collectAsState()
                FormScreenContent(
                    interactor = formInteractor,
                    eventReporter = eventReporter,
                    onClick = {
                        confirmationHelper.confirm()
                    },
                    onDisabledClick = sheetActivityStateHolder::requestValidation,
                    onProcessingCompleted = {
                        sheetActivityStateHolder.setResult(
                            EmbeddedActivityResult.Complete(
                                selection = null,
                                previousNewSelections = embeddedSelectionHolder.previousNewSelections,
                                hasBeenConfirmed = true,
                                customerState = customerStateHolder.customer.value,
                                shouldInvokeSelectionCallback = false,
                                launchMode = launchMode,
                            )
                        )
                    },
                    state = state,
                    updateSelection = embeddedSelectionHolder::setSelection,
                    savedPaymentMethodConfirmInteractorFactory = savedPaymentMethodConfirmInteractorFactory,
                )
            }

            override fun close() {
                formInteractor.close()
            }

            class Factory @Inject constructor(
                private val interactorFactory: EmbeddedFormInteractorFactory,
                private val eventReporter: EventReporter,
                private val sheetActivityStateHolder: SheetActivityStateHolder,
                private val confirmationHelper: SheetActivityConfirmationHelper,
                private val embeddedSelectionHolder: EmbeddedSelectionHolder,
                private val savedPaymentMethodConfirmInteractorFactory: SavedPaymentMethodConfirmInteractor.Factory,
                private val customerStateHolder: CustomerStateHolder,
            ) {
                fun create(launchMode: EmbeddedLaunchMode.Form): Form {
                    return create(
                        paymentMethodCode = launchMode.selectedPaymentMethodCode,
                        launchMode = launchMode,
                    )
                }

                fun create(
                    paymentMethodCode: PaymentMethodCode,
                    launchMode: EmbeddedLaunchMode,
                ): Form {
                    val hasSavedPaymentMethods = if (launchMode is EmbeddedLaunchMode.Form) {
                        customerStateHolder.paymentMethods.value.any {
                            it.type?.code == paymentMethodCode
                        }
                    } else {
                        customerStateHolder.paymentMethods.value.isNotEmpty()
                    }
                    return Form(
                        formInteractor = interactorFactory.create(
                            paymentMethodCode = paymentMethodCode,
                            hasSavedPaymentMethods = hasSavedPaymentMethods,
                        ),
                        eventReporter = eventReporter,
                        sheetActivityStateHolder = sheetActivityStateHolder,
                        confirmationHelper = confirmationHelper,
                        embeddedSelectionHolder = embeddedSelectionHolder,
                        savedPaymentMethodConfirmInteractorFactory = savedPaymentMethodConfirmInteractorFactory,
                        customerStateHolder = customerStateHolder,
                        launchMode = launchMode,
                    )
                }
            }
        }

        class VerticalPaymentOptions(
            private val interactor: PaymentMethodVerticalLayoutInteractor,
            private val isLiveMode: Boolean,
            private val sheetActivityState: StateFlow<SheetActivityStateHolder.State>,
            private val onContinueClick: () -> Unit,
            private val onDisabledClick: () -> Unit,
            private val onProcessingCompleted: () -> Unit,
        ) : Screen(), Closeable {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    isLiveMode = isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )

            override fun title(): StateFlow<ResolvableString?> = stateFlowOf(
                R.string.stripe_paymentsheet_select_your_payment_method.resolvableString
            )

            override fun isPerformingNetworkOperation(): StateFlow<Boolean> = stateFlowOf(false)

            @Composable
            override fun Content() {
                PaymentMethodVerticalLayoutUI(
                    interactor = interactor,
                    modifier = Modifier.padding(MaterialTheme.stripeFormInsets.getOuterFormInsets()),
                )
                val state by sheetActivityState.collectAsState()
                USBankAccountMandate(state)
                FormActivityError(state)
                Spacer(Modifier.height(40.dp))
                FormActivityPrimaryButton(
                    state = state,
                    onClick = onContinueClick,
                    onDisabledClick = onDisabledClick,
                    onProcessingCompleted = onProcessingCompleted,
                )
                PaymentSheetContentPadding()
            }

            override fun close() {
                interactor.close()
            }
        }

        class HorizontalPaymentOptions(
            private val interactor: AddPaymentMethodInteractor,
            private val eventReporter: EventReporter,
            private val sheetActivityState: StateFlow<SheetActivityStateHolder.State>,
            private val onContinueClick: () -> Unit,
            private val onDisabledClick: () -> Unit,
            private val onProcessingCompleted: () -> Unit,
        ) : Screen(), Closeable {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> = stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )

            override fun title(): StateFlow<ResolvableString?> = interactor.state.mapAsStateFlow { state ->
                if (state.supportedPaymentMethods.isOnlyOneNonCardPaymentMethod()) {
                    null
                } else {
                    state.supportedPaymentMethods.addPaymentMethodTitle()
                }
            }

            override fun isPerformingNetworkOperation(): StateFlow<Boolean> = stateFlowOf(false)

            @Composable
            override fun Content() {
                EventReporterProvider(eventReporter) {
                    AddPaymentMethod(interactor = interactor)
                    val state by sheetActivityState.collectAsState()
                    USBankAccountMandate(state)
                    FormActivityError(state)
                    Spacer(Modifier.height(40.dp))
                    FormActivityPrimaryButton(
                        state = state,
                        onClick = onContinueClick,
                        onDisabledClick = onDisabledClick,
                        onProcessingCompleted = onProcessingCompleted,
                    )
                    PaymentSheetContentPadding()
                }
            }

            override fun close() {
                interactor.close()
            }
        }

        class HorizontalSavedPaymentOptions(
            private val mutator: SavedPaymentMethodMutator,
            private val selection: StateFlow<PaymentSelection?>,
            private val cvcControllerFlow: StateFlow<CvcController>?,
            private val sheetActivityState: StateFlow<SheetActivityStateHolder.State>,
            private val isLiveMode: Boolean,
            private val onAddCardPressed: () -> Unit,
            private val onItemSelected: (PaymentSelection?) -> Unit,
            private val onContinueClick: () -> Unit,
            private val onDisabledClick: () -> Unit,
            private val onProcessingCompleted: () -> Unit,
        ) : Screen() {
            override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
                return combineAsStateFlow(mutator.editing, mutator.canEdit) { isEditing, canEdit ->
                    PaymentSheetTopBarStateFactory.create(
                        isLiveMode = isLiveMode,
                        editable = PaymentSheetTopBarState.Editable.Maybe(
                            isEditing = isEditing,
                            canEdit = canEdit,
                            onEditIconPressed = mutator::toggleEditing,
                        ),
                    )
                }
            }

            override fun title(): StateFlow<ResolvableString?> = stateFlowOf(
                R.string.stripe_paymentsheet_select_your_payment_method.resolvableString
            )

            override fun isPerformingNetworkOperation(): StateFlow<Boolean> {
                return sheetActivityState.mapAsStateFlow { it.isProcessing }
            }

            @Composable
            override fun Content() {
                val items by mutator.paymentOptionsItems.collectAsState()
                val currentSelection by selection.collectAsState()
                val isEditing by mutator.editing.collectAsState()
                val state by sheetActivityState.collectAsState()
                val requiresCvc = cvcControllerFlow != null &&
                    (currentSelection as? PaymentSelection.Saved)?.paymentMethod?.type == PaymentMethod.Type.Card
                SavedPaymentMethodTabLayoutUI(
                    paymentOptionsItems = items,
                    selectedPaymentOptionsItem = PaymentOptionsStateFactory.getSelectedItem(
                        items = items,
                        currentSelection = currentSelection,
                    ),
                    linkBrand = items.filterIsInstance<PaymentOptionsItem.Link>()
                        .firstOrNull()?.linkBrand ?: com.stripe.android.model.LinkBrand.Link,
                    isEditing = isEditing,
                    isProcessing = state.isProcessing,
                    onAddCardPressed = onAddCardPressed,
                    onItemSelected = onItemSelected,
                    onModifyItem = mutator::updatePaymentMethod,
                )
                val isCvcComplete = if (requiresCvc) {
                    cvcControllerFlow?.value?.isComplete?.collectAsState()?.value == true
                } else {
                    true
                }
                if (requiresCvc) {
                    CvcRecollectionField(
                        cvcControllerFlow = requireNotNull(cvcControllerFlow),
                        isProcessing = state.isProcessing,
                    )
                }
                Spacer(Modifier.height(40.dp))
                FormActivityPrimaryButton(
                    state = state.copy(isEnabled = state.isEnabled && isCvcComplete),
                    onClick = onContinueClick,
                    onDisabledClick = onDisabledClick,
                    onProcessingCompleted = onProcessingCompleted,
                )
                PaymentSheetContentPadding()
            }
        }
    }

    sealed class Action {
        object Back : Action()

        data class Close(val shouldInvokeRowSelectionCallback: Boolean = false) : Action()

        data class GoToScreen(val screen: Screen) : Action()
    }
}
