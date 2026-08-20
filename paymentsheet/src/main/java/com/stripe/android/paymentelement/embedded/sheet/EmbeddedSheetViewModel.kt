package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
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
        private val argsSupplier: () -> EmbeddedActivityArgs,
        private val sheetActivityArgs: SheetActivityArgs,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argsSupplier()
            val component = DaggerEmbeddedSheetComponent.factory().build(
                paymentMethodMetadata = args.paymentMethodMetadata,
                statusBarColor = args.statusBarColor,
                configuration = args.configuration,
                paymentElementCallbackIdentifier = args.paymentElementCallbackIdentifier,
                application = extras.requireApplication(),
                savedStateHandle = savedStateHandle,
                promotions = args.promotions,
                launchMode = args.launchMode,
                sheetActivityArgs = sheetActivityArgs,
            )

            component.customerStateHolder.setCustomerState(args.customerState)
            component.selectionHolder.setPreviousNewSelections(args.previousNewSelections)
            component.selectionHolder.setSelection(args.selection)

            return component.viewModel as T
        }
    }
}
