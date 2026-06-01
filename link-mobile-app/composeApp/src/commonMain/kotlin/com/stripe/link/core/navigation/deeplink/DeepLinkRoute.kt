package com.stripe.link.core.navigation.deeplink

import com.stripe.link.core.navigation.Coordinator

/**
 * Represents a parsed deep link destination.
 */
sealed class DeepLinkRoute {
    /** Navigation actions associated with this route. Empty list means unrecognized link. */
    abstract val actions: List<Coordinator.Action>

    /** Wallet / home screen. */
    data object Wallet : DeepLinkRoute() {
        override val actions: List<Coordinator.Action> = emptyList()
    }

    /** Unrecognized or unsupported deep link. */
    data object Unknown : DeepLinkRoute() {
        override val actions: List<Coordinator.Action> = emptyList()
    }

    companion object {
        /** Parses [url] and returns the corresponding [DeepLinkRoute]. */
        fun fromPath(url: String): DeepLinkRoute {
            return when {
                url.contains("/wallet") -> Wallet
                else -> Unknown
            }
        }
    }
}
