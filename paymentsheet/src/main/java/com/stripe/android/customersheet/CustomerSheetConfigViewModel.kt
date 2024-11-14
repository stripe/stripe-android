package com.stripe.android.customersheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

internal class CustomerSheetConfigViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    var configureRequest: CustomerSheetConfigureRequest?
        set(value) {
            savedStateHandle[CUSTOMER_SHEET_CONFIGURE_REQUEST_KEY] = value
        }
        get() = savedStateHandle[CUSTOMER_SHEET_CONFIGURE_REQUEST_KEY]

    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return CustomerSheetConfigViewModel(
                savedStateHandle = extras.createSavedStateHandle(),
            ) as T
        }
    }

    private companion object {
        private const val CUSTOMER_SHEET_CONFIGURE_REQUEST_KEY = "CustomerSheetConfigureRequest"
    }
}
