package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.annotations.VisibleForTesting

internal interface ManageScreenInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val paymentMethods: List<DisplayableSavedPaymentMethod>,
        val currentSelection: DisplayableSavedPaymentMethod?,
        val isEditing: Boolean,
    )

    sealed class ViewAction {
        data class SelectPaymentMethod(val paymentMethod: DisplayableSavedPaymentMethod) : ViewAction()
    }
}

internal class DefaultManageScreenInteractor(private val viewModel: BaseSheetViewModel) : ManageScreenInteractor {
    override val state = combineAsStateFlow(
        viewModel.paymentMethods,
        viewModel.paymentMethodMetadata,
        viewModel.selection,
        viewModel.editing,
    ) { paymentMethods, paymentMethodMetadata, paymentSelection, editing ->
        computeInitialState(
            paymentMethods,
            paymentMethodMetadata,
            paymentSelection,
            viewModel::providePaymentMethodName,
            editing,
        )
    }

    override fun handleViewAction(viewAction: ManageScreenInteractor.ViewAction) {
        when (viewAction) {
            is ManageScreenInteractor.ViewAction.SelectPaymentMethod ->
                handlePaymentMethodSelected(viewAction.paymentMethod)
        }
    }

    private fun handlePaymentMethodSelected(paymentMethod: DisplayableSavedPaymentMethod) {
        viewModel.handlePaymentMethodSelected(PaymentSelection.Saved(paymentMethod.paymentMethod))
        viewModel.handleBackPressed()
    }

    companion object {
        @VisibleForTesting
        internal fun computeInitialState(
            paymentMethods: List<PaymentMethod>?,
            paymentMethodMetadata: PaymentMethodMetadata?,
            selection: PaymentSelection?,
            providePaymentMethodName: (PaymentMethodCode?) -> String,
            isEditing: Boolean,
        ): ManageScreenInteractor.State {
            val displayablePaymentMethods = paymentMethods?.map {
                DisplayableSavedPaymentMethod(
                    displayName = providePaymentMethodName(it.type?.code),
                    paymentMethod = it,
                    isCbcEligible = paymentMethodMetadata?.cbcEligibility is CardBrandChoiceEligibility.Eligible,
                )
            } ?: emptyList()
            val currentSelection = paymentSelectionToDisplayableSavedPaymentMethod(selection, displayablePaymentMethods)
            return ManageScreenInteractor.State(displayablePaymentMethods, currentSelection, isEditing)
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
