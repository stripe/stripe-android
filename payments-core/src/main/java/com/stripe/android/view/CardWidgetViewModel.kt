package com.stripe.android.view

import android.view.View
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.BuildConfig.DEBUG
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal class CardWidgetViewModel(
    private val cbcEnabled: CbcEnabledProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _isCbcEligible = MutableStateFlow(false)
    val isCbcEligible: StateFlow<Boolean> = _isCbcEligible

    init {
        viewModelScope.launch(dispatcher) {
            _isCbcEligible.value = cbcEnabled() && determineCbcEligibility()
        }
    }

    private suspend fun determineCbcEligibility(): Boolean {
        // TODO(tillh-stripe) Query /wallets-config here
        // TODO(tillh-stripe) Make sure we don't use an outdated PaymentConfiguration
        delay(1.seconds)
        return DEBUG
    }

    class Factory(
        private val cbcEnabledProvider: CbcEnabledProvider,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return CardWidgetViewModel(cbcEnabledProvider) as T
        }
    }
}

internal fun View.doWithCardWidgetViewModel(
    viewModelStoreOwner: ViewModelStoreOwner? = null,
    action: LifecycleOwner.(CardWidgetViewModel) -> Unit,
) {
    doOnAttach {
        val lifecycleOwner = findViewTreeLifecycleOwner()
        val storeOwner = viewModelStoreOwner ?: findViewTreeViewModelStoreOwner()

        if (lifecycleOwner == null || storeOwner == null) {
            if (DEBUG) {
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
            owner = storeOwner,
            factory = factory,
        )[CardWidgetViewModel::class.java]

        lifecycleOwner.action(viewModel)
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
