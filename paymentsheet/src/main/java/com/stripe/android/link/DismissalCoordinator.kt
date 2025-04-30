package com.stripe.android.link

import javax.inject.Inject

internal interface DismissalCoordinator {
    val canDismiss: Boolean
    fun setDismissible(dismissible: Boolean)
}

internal inline fun <R> DismissalCoordinator.withDismissalDisabled(
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

internal class RealDismissalCoordinator @Inject constructor() : DismissalCoordinator {

    private var _canDismiss: Boolean = true

    override val canDismiss: Boolean
        get() = _canDismiss

    override fun setDismissible(dismissible: Boolean) {
        _canDismiss = dismissible
    }
}
