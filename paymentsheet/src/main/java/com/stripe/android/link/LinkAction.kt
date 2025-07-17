package com.stripe.android.link

import kotlinx.coroutines.flow.MutableSharedFlow

internal sealed interface LinkAction {
    data object BackPressed : LinkAction
    data object LogoutClicked : LinkAction
    data class DismissWithResult(val result: LinkActivityResult) : LinkAction
}

/**
 * Manages communication between Link screen ViewModels and the parent [LinkActivityViewModel]
 */
internal typealias LinkActions = MutableSharedFlow<LinkAction>
