package com.stripe.android.paymentsheet.example.playground.embedded

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.network.PlaygroundRequester
import kotlinx.coroutines.flow.StateFlow

internal class EmbeddedPlaygroundViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val confirming: StateFlow<Boolean> = savedStateHandle.getStateFlow(CONFIRMING_KEY, false)

    fun setConfirming(confirming: Boolean) {
        savedStateHandle[CONFIRMING_KEY] = confirming
    }

    internal suspend fun handleCreateIntentCallback(
        playgroundState: PlaygroundState,
        applicationContext: Context,
    ): CreateIntentResult {
        PlaygroundRequester(playgroundState.snapshot, applicationContext).fetch().fold(
            onSuccess = { state ->
                val clientSecret = requireNotNull(state.asPaymentState()).clientSecret
                return CreateIntentResult.Success(clientSecret)
            },
            onFailure = { exception ->
                return CreateIntentResult.Failure(IllegalStateException(exception))
            },
        )
    }

    companion object {
        private const val CONFIRMING_KEY = "CONFIRMING_KEY"
    }
}
