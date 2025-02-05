package com.stripe.android.paymentsheet.example.playground.embedded

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

internal class EmbeddedPlaygroundViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val confirming: StateFlow<Boolean> = savedStateHandle.getStateFlow(CONFIRMING_KEY, false)

    fun setConfirming(confirming: Boolean) {
        savedStateHandle[CONFIRMING_KEY] = confirming
    }

    companion object {
        private const val CONFIRMING_KEY = "CONFIRMING_KEY"
    }
}
