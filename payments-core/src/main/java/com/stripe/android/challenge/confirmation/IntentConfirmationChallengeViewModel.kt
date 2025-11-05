package com.stripe.android.challenge.confirmation

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.challenge.confirmation.di.DaggerIntentConfirmationChallengeComponent
import com.stripe.android.core.injection.UIContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class IntentConfirmationChallengeViewModel @Inject constructor(
    val bridgeHandler: ConfirmationChallengeBridgeHandler,
    @UIContext private val workContext: CoroutineContext = Dispatchers.Main,
) : ViewModel() {

    private val _showWebView = MutableStateFlow(false)
    val showWebView: StateFlow<Boolean> = _showWebView

    private val _result = MutableSharedFlow<IntentConfirmationChallengeActivityResult>()
    val result: SharedFlow<IntentConfirmationChallengeActivityResult> = _result

    init {
        viewModelScope.launch(workContext) {
            listenToEvents()
        }
    }

    private suspend fun listenToEvents() {
        bridgeHandler.event.collectLatest { event ->
            when (event) {
                is ConfirmationChallengeBridgeEvent.Ready -> {
                    _showWebView.emit(true)
                }
                is ConfirmationChallengeBridgeEvent.Success -> {
                    _result.emit(
                        IntentConfirmationChallengeActivityResult.Success(
                            clientSecret = event.clientSecret
                        )
                    )
                }
                is ConfirmationChallengeBridgeEvent.Error -> {
                    _result.emit(
                        IntentConfirmationChallengeActivityResult.Failed(
                            error = event.cause
                        )
                    )
                }
            }
        }
    }

    companion object {
        fun factory(savedStateHandle: SavedStateHandle? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = savedStateHandle ?: createSavedStateHandle()
                val app = this[APPLICATION_KEY] as Application
                val args: IntentConfirmationChallengeArgs = IntentConfirmationChallengeActivity.getArgs(handle)
                    ?: throw IllegalArgumentException("No IntentConfirmationChallengeArgs found")

                DaggerIntentConfirmationChallengeComponent
                    .builder()
                    .context(app)
                    .args(args)
                    .build()
                    .viewModel
            }
        }
    }
}
