package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?> = savedStateHandle.getStateFlow(
        SAVED_PM_SELECTION,
        initialValue = (selection.value as? PaymentSelection.Saved)?.paymentMethod
    )

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
    }

    fun updateMostRecentlySelectedSavedPaymentMethod(paymentMethod: PaymentMethod?) {
        savedStateHandle[SAVED_PM_SELECTION] = paymentMethod
    }

    companion object {
        const val SAVED_CUSTOMER = "customer_info"
        private const val SAVED_PM_SELECTION = "saved_selection"

        fun create(viewModel: BaseSheetViewModel): CustomerStateHolder {
            return CustomerStateHolder(
                savedStateHandle = viewModel.savedStateHandle,
                selection = viewModel.selection,
            )
        }
    }
}
