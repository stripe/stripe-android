package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo

/**
 * Valid user input into the inline sign up view.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class UserInput {
    /**
     * Represents an input that is valid for signing in to a link account.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class SignIn(
        val email: String
    ) : UserInput()

    /**
     * Represents an input that is valid for signing up to a link account.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class SignUp(
        val email: String,
        val phone: String,
        val country: String,
        val name: String?,
        val consentAction: SignUpConsentAction
    ) : UserInput()
}
