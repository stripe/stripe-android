package com.stripe.android.link

import javax.inject.Inject

/**
 * This coordinator sets the ability to dismiss the Link sheet. We want to avoid
 * dismissing the sheet while certain network requests are in progress.
 */
internal interface LinkDismissalCoordinator {
    val canDismiss: Boolean
    fun setDismissible(dismissible: Boolean)
}

/**
 * Runs the provided [action] and disables the dismissal of the Link sheet while it is running.
 */
internal inline fun <R> LinkDismissalCoordinator.withDismissalDisabled(
    action: () -> R,
): R {
    val originalDismissible = canDismiss
    setDismissible(false)
    try {
        return action()
    } finally {
        setDismissible(originalDismissible)
    }
}

internal class RealLinkDismissalCoordinator @Inject constructor() : LinkDismissalCoordinator {

    private var _canDismiss: Boolean = true

    override val canDismiss: Boolean
        get() = _canDismiss

    override fun setDismissible(dismissible: Boolean) {
        _canDismiss = dismissible
    }
}
