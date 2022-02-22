package com.stripe.android.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.robolectric.shadows.ShadowLooper.idleMainLooper

object TestUtils {
    @JvmStatic
    fun idleLooper() = idleMainLooper()

    fun viewModelFactoryFor(viewModel: ViewModel) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return viewModel as T
        }
    }
}
