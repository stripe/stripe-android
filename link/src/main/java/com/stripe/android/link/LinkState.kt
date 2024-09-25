package com.stripe.android.link

internal object LinkState

internal sealed interface LinkAction {
    data object BackPressed : LinkAction
    data object WalletClicked : LinkAction
}

internal sealed interface LinkResult {
    data class SendEffect(val effect: LinkEffect) : LinkResult
}

internal sealed interface LinkEffect {
    data object GoBack : LinkEffect
    data class NavigateTo(val screen: LinkScreen) : LinkEffect
}
