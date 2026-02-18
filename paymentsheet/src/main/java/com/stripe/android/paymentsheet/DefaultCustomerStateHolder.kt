package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CustomerStateHolder.Companion.SAVED_CUSTOMER
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class DefaultCustomerStateHolder(
    private val customerMetadataPermissions: StateFlow<CustomerMetadata.Permissions?>,
    private val savedStateHandle: SavedStateHandle,
    private val selection: StateFlow<PaymentSelection?>,
) : CustomerStateHolder {
    override val customer: StateFlow<CustomerState?> = savedStateHandle
        .getStateFlow<CustomerState?>(CustomerStateHolder.SAVED_CUSTOMER, null)

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    override val paymentMethods: StateFlow<List<PaymentMethod>> = customer
        .mapAsStateFlow { state ->
            state?.paymentMethods ?: emptyList()
        }

    override val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?> = savedStateHandle.getStateFlow(
        CustomerStateHolder.SAVED_PM_SELECTION,
        initialValue = (selection.value as? PaymentSelection.Saved)?.paymentMethod
    )

    override val canRemoveDuplicate: StateFlow<Boolean> = combineAsStateFlow(
        customer,
        customerMetadataPermissions,
    ) { customerState, customerMetadataPermissions ->
        return@combineAsStateFlow customerMetadataPermissions?.canRemoveDuplicates ?: false
    }

    override val canRemove: StateFlow<Boolean> = combineAsStateFlow(
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

    override val canUpdateFullPaymentMethodDetails: StateFlow<Boolean> = customerMetadataPermissions.mapAsStateFlow {
        it?.canUpdateFullPaymentMethodDetails ?: false
    }

    override fun setCustomerState(customerState: CustomerState?) {
        savedStateHandle[SAVED_CUSTOMER] = customerState

        val currentSelection = mostRecentlySelectedSavedPaymentMethod.value
        val newSelection = customerState?.paymentMethods?.firstOrNull { it.id == currentSelection?.id }
        updateMostRecentlySelectedSavedPaymentMethod(newSelection)
    }

    override fun setDefaultPaymentMethod(paymentMethod: PaymentMethod?) {
        val newCustomer = customer.value?.copy(defaultPaymentMethodId = paymentMethod?.id)

        savedStateHandle[SAVED_CUSTOMER] = newCustomer
    }

    override fun updateMostRecentlySelectedSavedPaymentMethod(paymentMethod: PaymentMethod?) {
        savedStateHandle[CustomerStateHolder.SAVED_PM_SELECTION] = paymentMethod
    }

    override fun addPaymentMethod(paymentMethod: PaymentMethod) {
        val currentCustomer = customer.value ?: return
        val newCustomer = currentCustomer.copy(paymentMethods = currentCustomer.paymentMethods + paymentMethod)

        savedStateHandle[SAVED_CUSTOMER] = newCustomer
    }

    companion object {
        fun create(viewModel: BaseSheetViewModel): CustomerStateHolder {
            return DefaultCustomerStateHolder(
                savedStateHandle = viewModel.savedStateHandle,
                selection = viewModel.selection,
                customerMetadataPermissions = viewModel.paymentMethodMetadata.mapAsStateFlow {
                    it?.customerMetadata?.permissions
                }
            )
        }
    }
}
