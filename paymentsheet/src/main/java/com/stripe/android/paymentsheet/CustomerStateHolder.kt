package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class CustomerStateHolder(
    private val savedStateHandle: SavedStateHandle,
    private val selection: StateFlow<PaymentSelection?>,
) {
    val customer: StateFlow<CustomerState?> = savedStateHandle
        .getStateFlow<CustomerState?>(SAVED_CUSTOMER, null)

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    val paymentMethods: StateFlow<List<PaymentMethod>> = customer
        .mapAsStateFlow { state ->
            state?.paymentMethods ?: emptyList()
        }

    private val mostRecentlySelectedSavedPaymentMethodId: StateFlow<String?> = savedStateHandle.getStateFlow(
        SAVED_PM_SELECTION,
        initialValue = (selection.value as? PaymentSelection.Saved)?.paymentMethod?.id
    )

    // This will ensure we use the latest copy of the payment method
    val mostRecentlySelectedSavedPaymentMethod = paymentMethods.flatMapLatestAsStateFlow { paymentMethods ->
        mostRecentlySelectedSavedPaymentMethodId.mapAsStateFlow { id ->
            paymentMethods.firstOrNull { it.id == id }
        }
    }

    val canRemove: StateFlow<Boolean> = customer.mapAsStateFlow { customerState ->
        customerState?.run {
            val hasRemovePermissions = customerState.permissions.canRemovePaymentMethods
            val hasRemoveLastPaymentMethodPermissions = customerState.permissions.canRemoveLastPaymentMethod

            when (paymentMethods.size) {
                0 -> false
                1 -> hasRemoveLastPaymentMethodPermissions && hasRemovePermissions
                else -> hasRemovePermissions
            }
        } ?: false
    }

    fun setCustomerState(customerState: CustomerState?) {
        savedStateHandle[SAVED_CUSTOMER] = customerState

        val currentSelection = mostRecentlySelectedSavedPaymentMethod.value
        val newSelection = customerState?.paymentMethods?.firstOrNull { it.id == currentSelection?.id }
        updateMostRecentlySelectedSavedPaymentMethod(newSelection)
    }

    fun setDefaultPaymentMethod(paymentMethod: PaymentMethod?) {
        val newCustomer = customer.value?.copy(defaultPaymentMethodId = paymentMethod?.id)

        savedStateHandle[SAVED_CUSTOMER] = newCustomer
    }

    fun updateMostRecentlySelectedSavedPaymentMethod(paymentMethod: PaymentMethod?) {
        savedStateHandle[SAVED_PM_SELECTION] = paymentMethod?.id
    }

    companion object {
        const val SAVED_CUSTOMER = "customer_info"
        const val SAVED_PM_SELECTION = "saved_selection"

        fun create(viewModel: BaseSheetViewModel): CustomerStateHolder {
            return CustomerStateHolder(
                savedStateHandle = viewModel.savedStateHandle,
                selection = viewModel.selection,
            )
        }
    }
}
