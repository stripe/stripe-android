package com.stripe.android.utils

import android.view.View
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.BuildConfig
import com.stripe.android.view.CardWidgetViewModel
import com.stripe.android.view.RealCbcEnabledProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal fun <T : ViewModel> View.doWithViewModel(action: LifecycleOwner.(T) -> Unit) {
    doOnAttach {
        val lifecycleOwner = findViewTreeLifecycleOwner()
        val viewModelStoreOwner = findViewTreeViewModelStoreOwner()

        if (lifecycleOwner == null || viewModelStoreOwner == null) {
            if (BuildConfig.DEBUG) {
                if (lifecycleOwner == null) {
                    error("Couldn't find a LifecycleOwner for view")
                } else {
                    error("Couldn't find a ViewModelStoreOwner for view")
                }
            }
            return@doOnAttach
        }

        val factory = CardWidgetViewModel.Factory(
            cbcEnabledProvider = RealCbcEnabledProvider(),
        )

        val viewModel = ViewModelProvider(
            owner = viewModelStoreOwner,
            factory = factory,
        )[CardWidgetViewModel::class.java]

        @Suppress("UNCHECKED_CAST")
        lifecycleOwner.action(viewModel as T)
    }
}

context(LifecycleOwner)
internal inline fun <T> Flow<T>.launchAndCollect(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: (T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(minActiveState) {
            collect {
                action(it)
            }
        }
    }
}
