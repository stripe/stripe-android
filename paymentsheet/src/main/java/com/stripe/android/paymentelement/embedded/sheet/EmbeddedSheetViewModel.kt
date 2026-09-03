package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgsHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

internal class EmbeddedSheetViewModel(
    val component: EmbeddedSheetComponent,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
) : ViewModel() {
    fun updateArgs(args: EmbeddedActivityArgs): Boolean {
        return component.argsUpdater.update(args)
    }

    override fun onCleared() {
        customViewModelScope.cancel()
    }

    class Factory(
        private val argsSupplier: () -> EmbeddedActivityArgs,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argsSupplier()
            val argsHolder = EmbeddedActivityArgsHolder(args)
            val customViewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val component = DaggerEmbeddedSheetComponent.factory().build(
                argsHolder = argsHolder,
                application = extras.requireApplication(),
                savedStateHandle = extras.createSavedStateHandle(),
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
