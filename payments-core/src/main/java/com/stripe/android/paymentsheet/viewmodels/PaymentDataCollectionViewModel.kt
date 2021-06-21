package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class PaymentDataCollectionViewModel(
    application: Application
) : AndroidViewModel(application) {

    internal class Factory(
        private val applicationSupplier: () -> Application,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentDataCollectionViewModel(
                applicationSupplier()
            ) as T
        }
    }
}
