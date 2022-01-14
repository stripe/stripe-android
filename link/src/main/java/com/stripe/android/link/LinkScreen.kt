package com.stripe.android.link

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
