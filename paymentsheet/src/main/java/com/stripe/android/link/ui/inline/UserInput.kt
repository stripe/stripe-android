package com.stripe.android.link.ui.inline

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Valid user input into the inline sign up view.
 */
@Parcelize
internal sealed class UserInput : Parcelable {
    /**
     * Represents an input that is valid for signing in to a link account.
     */
    data class SignIn(
        val email: String
    ) : UserInput()

    /**
     * Represents an input that is valid for signing up to a link account.
     */
    data class SignUp(
        val email: String,
        val phone: String,
        val country: String,
        val name: String?,
        val consentAction: SignUpConsentAction
    ) : UserInput()
}
