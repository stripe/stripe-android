package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

internal interface ManageScreenInteractor {
    val isLiveMode: Boolean

    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    fun close()

    data class State(
        val paymentMethods: List<DisplayableSavedPaymentMethod>,
        val currentSelection: DisplayableSavedPaymentMethod?,
        val isEditing: Boolean,
        val canEdit: Boolean,
    ) {
        val title: ResolvableString
            get() {
                val title = if (isEditing) {
                    R.string.stripe_paymentsheet_manage_payment_methods
                } else {
                    R.string.stripe_paymentsheet_select_payment_method
                }

                return title.resolvableString
            }

        fun topBarState(interactor: ManageScreenInteractor): PaymentSheetTopBarState {
            return PaymentSheetTopBarStateFactory.create(
                isLiveMode = interactor.isLiveMode,
                editable = PaymentSheetTopBarState.Editable.Maybe(
                    isEditing = isEditing,
                    canEdit = canEdit,
                    onEditIconPressed = {
                        interactor.handleViewAction(ViewAction.ToggleEdit)
                    },
                ),
            )
        }
    }

    sealed class ViewAction {
        data class SelectPaymentMethod(val paymentMethod: DisplayableSavedPaymentMethod) : ViewAction()
        data class UpdatePaymentMethod(val paymentMethod: DisplayableSavedPaymentMethod) : ViewAction()
        data object ToggleEdit : ViewAction()
    }
}

internal class DefaultManageScreenInteractor(
    private val paymentMethods: StateFlow<List<PaymentMethod>>,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val selection: StateFlow<PaymentSelection?>,
    private val editing: StateFlow<Boolean>,
    private val canEdit: StateFlow<Boolean>,
    private val toggleEdit: () -> Unit,
    private val providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString,
    private val onSelectPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val onUpdatePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val navigateBack: (withDelay: Boolean) -> Unit,
    private val defaultPaymentMethodId: StateFlow<String?>,
    dispatcher: CoroutineContext = Dispatchers.Default,
) : ManageScreenInteractor {

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val hasNavigatedBack: AtomicBoolean = AtomicBoolean(false)

    private val displayableSavedPaymentMethods: StateFlow<List<DisplayableSavedPaymentMethod>> =
        combineAsStateFlow(paymentMethods, defaultPaymentMethodId) { paymentMethods, defaultPaymentMethodId ->
            paymentMethods.map {
                it.toDisplayableSavedPaymentMethod(
                    providePaymentMethodName,
                    paymentMethodMetadata,
                    defaultPaymentMethodId
                )
            }
        }

    override val isLiveMode: Boolean = paymentMethodMetadata.stripeIntent.isLiveMode

    override val state = combineAsStateFlow(
        displayableSavedPaymentMethods,
        selection,
        editing,
        canEdit,
    ) { displayablePaymentMethods, paymentSelection, editing, canEdit ->
        val currentSelection = if (editing) {
            null
        } else {
            paymentSelectionToDisplayableSavedPaymentMethod(paymentSelection, displayablePaymentMethods)
        }

        ManageScreenInteractor.State(
            paymentMethods = displayablePaymentMethods,
            currentSelection = currentSelection,
            isEditing = editing,
            canEdit = canEdit,
        )
    }

    init {
        coroutineScope.launch {
            state.collect { state ->
                if (!state.isEditing && !state.canEdit && state.paymentMethods.size == 1) {
                    handlePaymentMethodSelected(state.paymentMethods.first())
                }
            }
        }

        coroutineScope.launch {
            paymentMethods.collect { paymentMethods ->
                if (paymentMethods.isEmpty()) {
                    safeNavigateBack(false)
                }
            }
        }
    }

    override fun handleViewAction(viewAction: ManageScreenInteractor.ViewAction) {
        when (viewAction) {
            is ManageScreenInteractor.ViewAction.SelectPaymentMethod ->
                handlePaymentMethodSelected(viewAction.paymentMethod)
            is ManageScreenInteractor.ViewAction.UpdatePaymentMethod -> onUpdatePaymentMethod(viewAction.paymentMethod)
            ManageScreenInteractor.ViewAction.ToggleEdit -> toggleEdit()
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    private fun handlePaymentMethodSelected(paymentMethod: DisplayableSavedPaymentMethod) {
        onSelectPaymentMethod(paymentMethod)
        safeNavigateBack(true)
    }

    private fun safeNavigateBack(withDelay: Boolean) {
        if (!hasNavigatedBack.getAndSet(true)) {
            navigateBack(withDelay)
        }
    }

    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            customerStateHolder: CustomerStateHolder,
            savedPaymentMethodMutator: SavedPaymentMethodMutator,
        ): ManageScreenInteractor {
            return DefaultManageScreenInteractor(
                paymentMethods = customerStateHolder.paymentMethods,
                paymentMethodMetadata = paymentMethodMetadata,
                selection = viewModel.selection,
                editing = savedPaymentMethodMutator.editing,
                canEdit = savedPaymentMethodMutator.canEdit,
                toggleEdit = savedPaymentMethodMutator::toggleEditing,
                providePaymentMethodName = savedPaymentMethodMutator.providePaymentMethodName,
                onSelectPaymentMethod = {
                    val savedPmSelection = PaymentSelection.Saved(it.paymentMethod)
                    viewModel.updateSelection(savedPmSelection)
                    viewModel.eventReporter.onSelectPaymentOption(savedPmSelection)
                },
                onUpdatePaymentMethod = { savedPaymentMethodMutator.updatePaymentMethod(it) },
                navigateBack = { withDelay ->
                    if (withDelay) {
                        viewModel.navigationHandler.popWithDelay()
                    } else {
                        viewModel.navigationHandler.pop()
                    }
                },
                defaultPaymentMethodId = savedPaymentMethodMutator.defaultPaymentMethodId
            )
        }

        private fun paymentSelectionToDisplayableSavedPaymentMethod(
            selection: PaymentSelection?,
            displayableSavedPaymentMethods: List<DisplayableSavedPaymentMethod>
        ): DisplayableSavedPaymentMethod? {
            val currentSelectionId = when (selection) {
                null,
                is PaymentSelection.ExternalPaymentMethod,
                PaymentSelection.GooglePay,
                is PaymentSelection.Link,
                is PaymentSelection.New -> return null
                is PaymentSelection.Saved -> selection.paymentMethod.id
            }
            return displayableSavedPaymentMethods.find { it.paymentMethod.id == currentSelectionId }
        }
    }
}
