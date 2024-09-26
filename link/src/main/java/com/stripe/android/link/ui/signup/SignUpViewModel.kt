package com.stripe.android.link.ui.signup

import com.stripe.android.link.LinkViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class SignUpViewModel : LinkViewModel<SignUpViewState, SignUpAction, SignUpResult, SignUpEffect>(
    initialState = SignUpViewState()
) {
    override fun actionToResult(action: SignUpAction): Flow<SignUpResult> {
        return when (action) {
            SignUpAction.SignUpClicked -> handleSignUpClicked()
        }
    }

    private fun handleSignUpClicked() = flow {
        emit(SignUpResult.ShowLoader)
        emit(
            value = SignUpResult.SendEffect(
                effect = SignUpEffect.NavigateToWallet
            )
        )
    }

    override fun resultToState(currentState: SignUpViewState, result: SignUpResult): SignUpViewState {
        return when (result) {
            is SignUpResult.SendEffect -> currentState
            SignUpResult.ShowLoader -> {
                currentState.copy(loading = true)
            }
        }
    }

    override fun resultToEffect(result: SignUpResult): SignUpEffect? {
        return when (result) {
            is SignUpResult.SendEffect -> result.effect
            SignUpResult.ShowLoader -> null
        }
    }
}
