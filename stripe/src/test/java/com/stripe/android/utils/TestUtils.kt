package com.stripe.android.utils

import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.robolectric.Shadows

internal object TestUtils {
    @JvmStatic
    fun idleLooper() = Shadows.shadowOf(Looper.getMainLooper()).idle()

    fun viewModelFactoryFor(viewModel: ViewModel) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return viewModel as T
        }
    }
}
