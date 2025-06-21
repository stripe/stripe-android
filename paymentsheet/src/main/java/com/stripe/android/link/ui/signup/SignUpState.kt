package com.stripe.android.link.ui.signup

import androidx.annotation.RestrictTo

/**
 * Enum representing the state of the Sign Up screen.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class SignUpState {
    InputtingPrimaryField,
    VerifyingEmail,
    InputtingRemainingFields,
}
