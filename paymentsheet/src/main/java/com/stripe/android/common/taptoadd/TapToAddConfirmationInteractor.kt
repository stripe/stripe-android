package com.stripe.android.common.taptoadd

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface TapToAddConfirmationInteractor {
    val state: StateFlow<State>

    fun onAction(action: Action)

    sealed interface Action {
        data object PrimaryButtonPress : Action
    }

    data class State(
        val title: ResolvableString,
        val buttonLabel: ResolvableString,
        val state: ConfirmationState,
    ) {
        enum class ConfirmationState {
            Idle,
            Processing,
            Completed
        }
    }
}

internal class DefaultTapToAddConfirmationInteractor(
    coroutineScope: CoroutineScope,
    private val confirmationHandler: ConfirmationHandler,
): TapToAddConfirmationInteractor {
    private val _state = MutableStateFlow(
        TapToAddConfirmationInteractor.State(
            title = "Pay $10".resolvableString,
            buttonLabel = "Pay".resolvableString,
            state = TapToAddConfirmationInteractor.State.ConfirmationState.Idle,
        )
    )
    override val state = _state.asStateFlow()

    init {
        coroutineScope.launch {
            confirmationHandler.state.collectLatest { confirmationState ->
                _state.update { state ->
                    state.copy(
                        state = when (confirmationState) {
                            is ConfirmationHandler.State.Idle -> {
                                TapToAddConfirmationInteractor.State.ConfirmationState.Idle
                            }
                            is ConfirmationHandler.State.Confirming -> {
                                TapToAddConfirmationInteractor.State.ConfirmationState.Processing
                            }
                            is ConfirmationHandler.State.Complete -> {
                                TapToAddConfirmationInteractor.State.ConfirmationState.Completed
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onAction(action: TapToAddConfirmationInteractor.Action) {
        when (action) {
            is TapToAddConfirmationInteractor.Action.PrimaryButtonPress -> {
            }
        }
    }

    private companion object {

    }
}
