package com.stripe.android.link.ui.signup

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString

@Immutable
internal data class SignUpScreenState(
    val merchantName: String,
    val signUpEnabled: Boolean,
    val requiresNameCollection: Boolean,
    val signUpState: SignUpState = SignUpState.InputtingPrimaryField,
    val errorMessage: ResolvableString? = null
)
