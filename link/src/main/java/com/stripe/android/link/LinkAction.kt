package com.stripe.android.link

internal sealed interface LinkAction {
    data object BackPressed : LinkAction
}
