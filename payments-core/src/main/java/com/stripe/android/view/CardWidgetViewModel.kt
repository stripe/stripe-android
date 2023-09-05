package com.stripe.android.view

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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.BuildConfig.DEBUG
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal class CardWidgetViewModel(
    private val cbcEnabled: CbcEnabledProvider,
) : ViewModel() {

    private val _isCbcEligible = MutableStateFlow(false)
    val isCbcEligible: StateFlow<Boolean> = _isCbcEligible

    init {
        viewModelScope.launch {
            _isCbcEligible.value = cbcEnabled() && determineCbcEligibility()
        }
    }

    private suspend fun determineCbcEligibility(): Boolean {
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

internal fun View.cardWidgetViewModel(): Lazy<CardWidgetViewModel> {
    return lazy {
        val storeOwner = findViewTreeViewModelStoreOwner()!!
        val factory = CardWidgetViewModel.Factory(
            cbcEnabledProvider = RealCbcEnabledProvider(),
        )
        ViewModelProvider(storeOwner, factory)[CardWidgetViewModel::class.java]
    }
}

internal inline fun <T> Flow<T>.launchAndCollectIn(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: (T) -> Unit
) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(minActiveState) {
            collect {
                action(it)
            }
        }
    }
}

internal fun View.doWithLifecycleOwner(action: (LifecycleOwner) -> Unit) {
    doOnAttach {
        val owner = findViewTreeLifecycleOwner()
        if (DEBUG && owner == null) {
            error("Couldn't find a LifecycleOwner for view")
        }
        owner?.let(action)
    }
}
