package com.stripe.android.common.taptoadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import javax.inject.Inject

internal class TapToAddViewModel @Inject constructor(
    val component: TapToAddViewModelComponent,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        customViewModelScope.cancel()
    }

    class Factory(
        private val argSupplier: () -> TapToAddContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argSupplier()
            val component = DaggerTapToAddViewModelComponent.factory().build(
                paymentMethodMetadata = args.paymentMethodMetadata,
                tapToAddMode = args.mode,
                savedStateHandle = extras.createSavedStateHandle(),
                application = extras.requireApplication(),
                paymentElementCallbackIdentifier = args.paymentElementCallbackIdentifier,
                productUsage = args.productUsage,
            )

            return component.viewModel as T
        }
    }
}
