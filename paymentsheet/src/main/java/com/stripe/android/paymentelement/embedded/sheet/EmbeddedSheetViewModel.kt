package com.stripe.android.paymentelement.embedded.sheet

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
    @ViewModelScope private val customViewModelScope: CoroutineScope,
) : ViewModel() {
    override fun onCleared() {
        customViewModelScope.cancel()
    }

    class Factory(
        private val argsSupplier: () -> EmbeddedSheetContract.Args,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argsSupplier()
            val component = DaggerEmbeddedSheetComponent.factory().build(
                mode = args.mode,
                paymentMethodMetadata = args.paymentMethodMetadata,
                selectedPaymentMethodCode = args.selectedPaymentMethodCode,
                hasSavedPaymentMethods = args.hasSavedPaymentMethods,
                statusBarColor = args.statusBarColor,
                configuration = args.configuration,
                paymentElementCallbackIdentifier = args.paymentElementCallbackIdentifier,
                application = extras.requireApplication(),
                savedStateHandle = extras.createSavedStateHandle(),
                promotion = args.promotion,
            )

            component.selectionHolder.set(args.selection)
            component.customerStateHolder.setCustomerState(args.customerState)

            return component.viewModel as T
        }
    }
}
