package com.stripe.android.link.ui.inline

/**
 * Valid user input into the inline sign up view.
 */
@Deprecated(
    message = "This isn't meant for public usage and will be removed in a future release.",
)
sealed class UserInput {

    /**
     * Represents an input that is valid for signing in to a link account.
     */
    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release.",
    )
    data class SignIn(
        val email: String
    ) : UserInput()

    /**
     * Represents an input that is valid for signing up to a link account.
     */
    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release.",
    )
    data class SignUp(
        val email: String,
        val phone: String,
        val country: String,
        val name: String?
    ) : UserInput()
}
