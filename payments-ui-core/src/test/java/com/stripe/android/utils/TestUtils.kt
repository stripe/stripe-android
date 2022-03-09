package com.stripe.android.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.robolectric.shadows.ShadowLooper

object TestUtils {
    @JvmStatic
    fun idleLooper() = ShadowLooper.idleMainLooper()

    fun viewModelFactoryFor(viewModel: ViewModel) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return viewModel as T
        }
    }
}
