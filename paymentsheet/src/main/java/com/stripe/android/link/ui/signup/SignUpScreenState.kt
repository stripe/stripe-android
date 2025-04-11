package com.stripe.android.link.ui.signup

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.account.LinkAuthResult

@Immutable
internal data class SignUpScreenState(
    val merchantName: String,
    val signUpEnabled: Boolean,
    val requiresNameCollection: Boolean,
    val showKeyboardOnOpen: Boolean,
    val signUpState: SignUpState = SignUpState.InputtingPrimaryField,
    val submitState: SubmitState = SubmitState.Idle,
    val errorMessage: ResolvableString? = null
) {
    val canEditForm: Boolean
        get() = submitState == SubmitState.Idle

    enum class SubmitState {
        Idle,
        Submitting,
        Success
    }
}
