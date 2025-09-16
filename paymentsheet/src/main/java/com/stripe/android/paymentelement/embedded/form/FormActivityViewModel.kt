package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import javax.inject.Inject

internal class FormActivityViewModel @Inject constructor(
    val component: FormActivityViewModelComponent,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
    private val confirmationHandler: ConfirmationHandler,
    private val paymentMethodMetadata: PaymentMethodMetadata
) : ViewModel() {

    private var isBootstrapped = false

    fun inject(activity: FormActivity) {
        component.subcomponentFactory.build(
            activityResultCaller = activity,
            lifecycleOwner = activity
        ).inject(activity)
        bootstrapConfirmationHandler()
    }

    private fun bootstrapConfirmationHandler() {
        if (!isBootstrapped) {
            confirmationHandler.bootstrap(paymentMethodMetadata)
            isBootstrapped = true
        }
    }

    override fun onCleared() {
        customViewModelScope.cancel()
    }

    class Factory(
        private val argSupplier: () -> FormContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argSupplier()
            val component = DaggerFormActivityViewModelComponent.factory().build(
                paymentMethodMetadata = args.paymentMethodMetadata,
                selectedPaymentMethodCode = args.selectedPaymentMethodCode,
                hasSavedPaymentMethods = args.hasSavedPaymentMethods,
                configuration = args.configuration,
                initializationMode = args.initializationMode,
                statusBarColor = args.statusBarColor,
                application = extras.requireApplication(),
                paymentElementCallbackIdentifier = args.paymentElementCallbackIdentifier,
                savedStateHandle = extras.createSavedStateHandle(),
            )

            component.selectionHolder.set(args.paymentSelection)

            return component.viewModel as T
        }
    }
}
