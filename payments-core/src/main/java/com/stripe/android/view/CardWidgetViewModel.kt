package com.stripe.android.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.BuildConfig.DEBUG
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
