package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import javax.inject.Inject

internal class EmbeddedSheetViewModel @Inject constructor(
    val component: EmbeddedSheetComponent,
    val navigator: EmbeddedSheetNavigator,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
) : ViewModel() {

    override fun onCleared() {
        navigator.closeScreens()
        customViewModelScope.cancel()
    }

    class Factory(
        private val argSupplier: () -> EmbeddedSheetArgs
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argSupplier()
            val savedStateHandle = extras.createSavedStateHandle()
            val application = extras.requireApplication()

            val paymentMethodMetadata = when (args) {
                is EmbeddedSheetArgs.Form -> args.formArgs.paymentMethodMetadata
                is EmbeddedSheetArgs.Manage -> args.manageArgs.paymentMethodMetadata
            }
            val paymentElementCallbackIdentifier = when (args) {
                is EmbeddedSheetArgs.Form -> args.formArgs.paymentElementCallbackIdentifier
                is EmbeddedSheetArgs.Manage -> args.manageArgs.paymentElementCallbackIdentifier
            }

            val component = DaggerEmbeddedSheetComponent.factory().build(
                paymentMethodMetadata = paymentMethodMetadata,
                paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                application = application,
                savedStateHandle = savedStateHandle,
                embeddedSheetArgs = args,
            )

            // Initialize selection and customer state from args
            when (args) {
                is EmbeddedSheetArgs.Form -> {
                    component.selectionHolder.set(args.formArgs.paymentSelection)
                    args.formArgs.customerState?.let {
                        component.customerStateHolder.setCustomerState(it)
                    }
                }
                is EmbeddedSheetArgs.Manage -> {
                    component.selectionHolder.set(args.manageArgs.selection)
                    component.customerStateHolder.setCustomerState(args.manageArgs.customerState)
                }
            }

            return component.viewModel as T
        }
    }
}
