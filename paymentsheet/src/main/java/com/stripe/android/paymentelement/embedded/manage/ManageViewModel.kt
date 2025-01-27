package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import javax.inject.Inject

internal class ManageViewModel @Inject constructor(
    val customerStateHolder: CustomerStateHolder,
    val selectionHolder: EmbeddedSelectionHolder,
    val manageNavigator: ManageNavigator,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
) : ViewModel() {
    override fun onCleared() {
        customViewModelScope.cancel()
    }

    class Factory(
        private val argsSupplier: () -> ManageContract.Args,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()

            val args = argsSupplier()

            val component = DaggerManageComponent.builder()
                .savedStateHandle(savedStateHandle)
                .paymentMethodMetadata(args.paymentMethodMetadata)
                .context(extras.requireApplication())
                .build()

            component.customerStateHolder.setCustomerState(args.customerState)
            component.selectionHolder.set(args.selection)

            return component.viewModel as T
        }
    }
}
