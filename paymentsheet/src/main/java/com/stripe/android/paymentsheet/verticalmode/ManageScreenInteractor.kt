package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

internal interface ManageScreenInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val paymentMethods: List<DisplayableSavedPaymentMethod>,
        val currentSelection: DisplayableSavedPaymentMethod?,
        val isEditing: Boolean,
        val canDelete: Boolean,
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
    private val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    private val providePaymentMethodName: (PaymentMethodCode?) -> String,
    private val onSelectPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val onDeletePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val onEditPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val navigateBack: () -> Unit,
) : ManageScreenInteractor {

    constructor(viewModel: BaseSheetViewModel) : this(
        paymentMethods = viewModel.paymentMethods,
        paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
        selection = viewModel.selection,
        editing = viewModel.editing,
        allowsRemovalOfLastSavedPaymentMethod = viewModel.config.allowsRemovalOfLastSavedPaymentMethod,
        providePaymentMethodName = viewModel::providePaymentMethodName,
        onSelectPaymentMethod = { viewModel.handlePaymentMethodSelected(PaymentSelection.Saved(it.paymentMethod)) },
        onDeletePaymentMethod = { viewModel.removePaymentMethod(it.paymentMethod) },
        onEditPaymentMethod = { viewModel.modifyPaymentMethod(it.paymentMethod) },
        navigateBack = viewModel::handleBackPressed
    )

    private val hasNavigatedBack: AtomicBoolean = AtomicBoolean(false)

    override val state = combineAsStateFlow(
        paymentMethods,
        selection,
        editing,
    ) { paymentMethods, paymentSelection, editing ->
        val displayablePaymentMethods = paymentMethods?.map {
            it.toDisplayableSavedPaymentMethod(providePaymentMethodName, paymentMethodMetadata)
        } ?: emptyList()

        if (noPaymentMethodsAvailableToManage(displayablePaymentMethods)) {
            safeNavigateBack()
        }

        val canDelete = displayablePaymentMethods.size > 1 || allowsRemovalOfLastSavedPaymentMethod

        val currentSelection = if (editing) {
            null
        } else {
            paymentSelectionToDisplayableSavedPaymentMethod(paymentSelection, displayablePaymentMethods)
        }

        ManageScreenInteractor.State(displayablePaymentMethods, currentSelection, editing, canDelete = canDelete)
    }

    override fun handleViewAction(viewAction: ManageScreenInteractor.ViewAction) {
        when (viewAction) {
            is ManageScreenInteractor.ViewAction.SelectPaymentMethod ->
                handlePaymentMethodSelected(viewAction.paymentMethod)
            is ManageScreenInteractor.ViewAction.DeletePaymentMethod -> onDeletePaymentMethod(viewAction.paymentMethod)
            is ManageScreenInteractor.ViewAction.EditPaymentMethod -> onEditPaymentMethod(viewAction.paymentMethod)
        }
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
