package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
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
        val canDelete: Boolean,
        val canEdit: Boolean,
    )

    sealed class ViewAction {
        data class SelectPaymentMethod(val paymentMethod: DisplayableSavedPaymentMethod) : ViewAction()
        data class DeletePaymentMethod(val paymentMethod: DisplayableSavedPaymentMethod) : ViewAction()
        data class EditPaymentMethod(val paymentMethod: DisplayableSavedPaymentMethod) : ViewAction()
    }
}

internal class DefaultManageScreenInteractor(
    private val paymentMethods: StateFlow<List<PaymentMethod>?>,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val selection: StateFlow<PaymentSelection?>,
    private val editing: StateFlow<Boolean>,
    private val canEdit: StateFlow<Boolean>,
    private val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    private val providePaymentMethodName: (PaymentMethodCode?) -> String,
    private val onSelectPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val onDeletePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val onEditPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val navigateBack: () -> Unit,
    override val isLiveMode: Boolean,
    dispatcher: CoroutineContext = Dispatchers.Default,
) : ManageScreenInteractor {

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val hasNavigatedBack: AtomicBoolean = AtomicBoolean(false)

    private val displayableSavedPaymentMethods: StateFlow<List<DisplayableSavedPaymentMethod>> =
        paymentMethods.mapAsStateFlow { paymentMethods ->
            paymentMethods?.map {
                it.toDisplayableSavedPaymentMethod(providePaymentMethodName, paymentMethodMetadata)
            } ?: emptyList()
        }

    override val state = combineAsStateFlow(
        displayableSavedPaymentMethods,
        selection,
        editing,
        canEdit,
    ) { displayablePaymentMethods, paymentSelection, editing, canEdit ->
        val canDelete = displayablePaymentMethods.size > 1 || allowsRemovalOfLastSavedPaymentMethod

        val currentSelection = if (editing) {
            null
        } else {
            paymentSelectionToDisplayableSavedPaymentMethod(paymentSelection, displayablePaymentMethods)
        }

        ManageScreenInteractor.State(
            paymentMethods = displayablePaymentMethods,
            currentSelection = currentSelection,
            isEditing = editing,
            canDelete = canDelete,
            canEdit = canEdit,
        )
    }

    init {
        coroutineScope.launch {
            displayableSavedPaymentMethods.collect {
                if (noPaymentMethodsAvailableToManage(it)) {
                    safeNavigateBack()
                }
            }
        }

        coroutineScope.launch {
            editing.collect { editing ->
                val savedPaymentMethods = displayableSavedPaymentMethods.value
                val oneModifiablePmRemaining =
                    savedPaymentMethods.size == 1 && savedPaymentMethods.first().isModifiable()
                if (!editing && oneModifiablePmRemaining && !allowsRemovalOfLastSavedPaymentMethod) {
                    handlePaymentMethodSelected(savedPaymentMethods.first())
                    safeNavigateBack()
                }
            }
        }
    }

    override fun handleViewAction(viewAction: ManageScreenInteractor.ViewAction) {
        when (viewAction) {
            is ManageScreenInteractor.ViewAction.SelectPaymentMethod ->
                handlePaymentMethodSelected(viewAction.paymentMethod)
            is ManageScreenInteractor.ViewAction.DeletePaymentMethod -> onDeletePaymentMethod(viewAction.paymentMethod)
            is ManageScreenInteractor.ViewAction.EditPaymentMethod -> onEditPaymentMethod(viewAction.paymentMethod)
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    private fun handlePaymentMethodSelected(paymentMethod: DisplayableSavedPaymentMethod) {
        onSelectPaymentMethod(paymentMethod)
        safeNavigateBack()
    }

    private fun noPaymentMethodsAvailableToManage(
        displayableSavedPaymentMethods: List<DisplayableSavedPaymentMethod>
    ): Boolean {
        val onlyOneNotModifiablePm = displayableSavedPaymentMethods.size == 1 &&
            !allowsRemovalOfLastSavedPaymentMethod &&
            !displayableSavedPaymentMethods.first().isModifiable()
        return displayableSavedPaymentMethods.isEmpty() || onlyOneNotModifiablePm
    }

    private fun safeNavigateBack() {
        if (!hasNavigatedBack.getAndSet(true)) {
            navigateBack()
        }
    }

    companion object {
        fun create(viewModel: BaseSheetViewModel): ManageScreenInteractor {
            return DefaultManageScreenInteractor(
                paymentMethods = viewModel.savedPaymentMethodMutator.paymentMethods,
                paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
                selection = viewModel.selection,
                editing = viewModel.editing,
                canEdit = viewModel.savedPaymentMethodMutator.canEdit,
                allowsRemovalOfLastSavedPaymentMethod = viewModel.config.allowsRemovalOfLastSavedPaymentMethod,
                providePaymentMethodName = viewModel::providePaymentMethodName,
                onSelectPaymentMethod = {
                    viewModel.handlePaymentMethodSelected(PaymentSelection.Saved(it.paymentMethod))
                },
                onDeletePaymentMethod = { viewModel.savedPaymentMethodMutator.removePaymentMethod(it.paymentMethod) },
                onEditPaymentMethod = { viewModel.savedPaymentMethodMutator.modifyPaymentMethod(it.paymentMethod) },
                navigateBack = viewModel::handleBackPressed,
                isLiveMode = requireNotNull(viewModel.paymentMethodMetadata.value).stripeIntent.isLiveMode,
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
                PaymentSelection.Link,
                is PaymentSelection.New -> return null
                is PaymentSelection.Saved -> selection.paymentMethod.id
            }
            return displayableSavedPaymentMethods.find { it.paymentMethod.id == currentSelectionId }
        }
    }
}
