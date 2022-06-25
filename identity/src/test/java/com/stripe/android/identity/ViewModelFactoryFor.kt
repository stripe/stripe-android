package com.stripe.android.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal fun viewModelFactoryFor(viewModel: ViewModel) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewModel as T
    }
}
