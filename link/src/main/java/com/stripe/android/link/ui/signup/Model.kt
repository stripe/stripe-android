package com.stripe.android.link.ui.signup

data class SignUpViewState(
    val loading: Boolean = false
)

sealed interface SignUpAction {
    data object SignUpClicked : SignUpAction
}

sealed interface SignUpResult {
    data object ShowLoader : SignUpResult
    data class SendEffect(val effect: SignUpEffect) : SignUpResult
}

sealed interface SignUpEffect {
    data object NavigateToWallet : SignUpEffect
}
