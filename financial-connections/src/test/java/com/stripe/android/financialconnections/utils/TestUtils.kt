package com.stripe.android.financialconnections.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
internal object TestUtils {
    @Suppress("UNCHECKED_CAST")
    fun viewModelFactoryFor(viewModel: ViewModel) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return viewModel as T
        }
    }
}
