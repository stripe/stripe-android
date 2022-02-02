package com.stripe.android.link

/**
 * All Link screens.
 */
internal sealed class LinkScreen(
    val name: String
) {
    companion object {
        fun values() = listOf(SignUp)
    }

    object SignUp : LinkScreen(
        "SignUp"
    )
}
