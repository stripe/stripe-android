package com.stripe.android.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import org.robolectric.shadows.ShadowLooper.idleMainLooper

internal object TestUtils {
    @JvmStatic
    fun idleLooper() = idleMainLooper()

    fun viewModelFactoryFor(viewModel: ViewModel) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return viewModel as T
        }
    }

    fun <T> LiveData<BaseSheetViewModel.Event<T>?>.observeEventsForever(
        observer: (T) -> Unit,
    ) {
        observeForever { event ->
            val content = event?.getContentIfNotHandled() ?: return@observeForever
            observer(content)
        }
    }
}
