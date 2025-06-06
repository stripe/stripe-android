package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class CustomerStateHolder(
    private val customerMetadataPermissions: StateFlow<CustomerMetadata.Permissions?>,
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

    val canRemoveDuplicate: StateFlow<Boolean> = combineAsStateFlow(
        customer,
        customerMetadataPermissions,
    ) { customerState, customerMetadataPermissions ->
        return@combineAsStateFlow customerMetadataPermissions?.canRemoveDuplicates ?: false
    }

    val canRemove: StateFlow<Boolean> = combineAsStateFlow(
        paymentMethods,
        customerMetadataPermissions,
    ) { paymentMethods, customerMetadataPermissions ->
        customerMetadataPermissions?.run {
            val hasRemovePermissions = customerMetadataPermissions.canRemovePaymentMethods
            val hasRemoveLastPaymentMethodPermissions = customerMetadataPermissions.canRemoveLastPaymentMethod
            when (paymentMethods.size) {
                0 -> false
                1 -> hasRemoveLastPaymentMethodPermissions && hasRemovePermissions
                else -> hasRemovePermissions
            }
        } ?: false
    }

    val canUpdateFullPaymentMethodDetails: StateFlow<Boolean> = customerMetadataPermissions.mapAsStateFlow {
        it?.canUpdateFullPaymentMethodDetails ?: false
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
        savedStateHandle[SAVED_PM_SELECTION] = paymentMethod
    }

    companion object {
        const val SAVED_CUSTOMER = "customer_info"
        const val SAVED_PM_SELECTION = "saved_selection"

        fun create(viewModel: BaseSheetViewModel): CustomerStateHolder {
            return CustomerStateHolder(
                savedStateHandle = viewModel.savedStateHandle,
                selection = viewModel.selection,
                customerMetadataPermissions = viewModel.paymentMethodMetadata.mapAsStateFlow {
                    it?.customerMetadata?.permissions
                }
            )
        }
    }
}
