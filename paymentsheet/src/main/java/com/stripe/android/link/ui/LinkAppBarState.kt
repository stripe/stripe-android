package com.stripe.android.link.ui

import com.stripe.android.link.LinkScreen

internal data class LinkAppBarState(
    val showHeader: Boolean,
    val showOverflowMenu: Boolean,
    val canNavigateBack: Boolean,
) {

    val canShowCloseIcon: Boolean
        get() = !canNavigateBack

    internal companion object {

        fun initial(): LinkAppBarState {
            return LinkAppBarState(
                showHeader = true,
                showOverflowMenu = false,
                canNavigateBack = false,
            )
        }

        fun create(
            route: String?,
            previousEntryRoute: String?,
            consumerIsSigningUp: Boolean,
        ): LinkAppBarState {
            val showHeaderRoutes = mutableSetOf(
                LinkScreen.Loading.route,
                LinkScreen.SignUp.route,
                LinkScreen.Wallet.route,
                LinkScreen.Verification.route,
            )

            if (consumerIsSigningUp) {
                // If the consumer is just signing up, we show the Link logo on the payment method screen.
                showHeaderRoutes.add(LinkScreen.PaymentMethod.route)
            }

            return LinkAppBarState(
                showHeader = route in showHeaderRoutes,
                showOverflowMenu = route == LinkScreen.Wallet.route,
                canNavigateBack = previousEntryRoute != null,
            )
        }
    }
}
