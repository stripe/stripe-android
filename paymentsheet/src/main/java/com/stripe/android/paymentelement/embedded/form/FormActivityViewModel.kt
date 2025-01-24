package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import javax.inject.Inject

internal class FormActivityViewModel @Inject constructor(
    val paymentMethodMetadata: PaymentMethodMetadata,
    val selectedPaymentMethodCode: PaymentMethodCode
) : ViewModel() {
    class Factory(
        private val argSupplier: () -> FormContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val args = argSupplier()
            val component = DaggerFormActivityComponent.builder()
                .paymentMethodMetadata(args.paymentMethodMetadata)
                .selectedPaymentMethodCode(args.selectedPaymentMethodCode)
                .build()

            return component.viewModel as T
        }
    }
}
