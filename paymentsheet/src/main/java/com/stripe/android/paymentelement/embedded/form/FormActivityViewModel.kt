package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject

internal class FormActivityViewModel @Inject constructor(
    paymentMethodMetadata: PaymentMethodMetadata,
    selectedPaymentMethodCode: PaymentMethodCode,
    embeddedFormInteractorFactory: EmbeddedFormInteractorFactory,
    private val selectionHolder: EmbeddedSelectionHolder,
    val eventReporter: EventReporter
) : ViewModel() {

    val formInteractor = embeddedFormInteractorFactory.create(
        coroutineScope = viewModelScope,
        paymentMethodCode = selectedPaymentMethodCode,
        paymentMethodMetadata = paymentMethodMetadata,
        setSelection = ::setSelection
    )

    private fun setSelection(selection: PaymentSelection?) {
        selectionHolder.set(selection)
    }

    class Factory(
        private val argSupplier: () -> FormContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argSupplier()
            val component = DaggerFormActivityComponent.builder()
                .paymentMethodMetadata(args.paymentMethodMetadata)
                .selectedPaymentMethodCode(args.selectedPaymentMethodCode)
                .context(extras.requireApplication())
                .savedStateHandle(extras.createSavedStateHandle())
                .build()

            return component.viewModel as T
        }
    }
}
