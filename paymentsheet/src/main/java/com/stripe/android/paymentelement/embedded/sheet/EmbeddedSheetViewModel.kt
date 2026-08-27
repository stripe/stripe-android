package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

internal class EmbeddedSheetViewModel(
    val component: EmbeddedSheetComponent,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
) : ViewModel() {
    override fun onCleared() {
        customViewModelScope.cancel()
    }

    class Factory(
        private val argsSupplier: () -> EmbeddedActivityArgs,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argsSupplier()
            val customViewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val component = DaggerEmbeddedSheetComponent.factory().build(
                paymentMethodMetadata = args.paymentMethodMetadata,
                statusBarColor = args.statusBarColor,
                configuration = args.configuration,
                productUsage = args.productUsage,
                paymentElementCallbackIdentifier = args.paymentElementCallbackIdentifier,
                application = extras.requireApplication(),
                savedStateHandle = extras.createSavedStateHandle(),
                promotion = args.promotion,
                launchMode = args.launchMode,
                viewModelScope = customViewModelScope,
            )

            component.customerStateHolder.setCustomerState(args.customerState)
            component.selectionHolder.setPreviousNewSelections(args.previousNewSelections)
            component.selectionHolder.setSelection(args.selection)

            return EmbeddedSheetViewModel(
                component = component,
                customViewModelScope = customViewModelScope,
            ) as T
        }
    }
}
