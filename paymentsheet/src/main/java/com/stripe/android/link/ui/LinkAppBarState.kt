package com.stripe.android.link.ui

import androidx.annotation.DrawableRes
import com.stripe.android.link.LinkScreen
import com.stripe.android.paymentsheet.R

internal data class LinkAppBarState(
    @DrawableRes val navigationIcon: Int,
    val showHeader: Boolean,
    val showOverflowMenu: Boolean,
    val email: String?,
) {

    internal companion object {

        fun initial(): LinkAppBarState {
            return LinkAppBarState(
                navigationIcon = R.drawable.stripe_link_close,
                showHeader = true,
                showOverflowMenu = false,
                email = null,
            )
        }

        fun create(
            route: String?,
            email: String?,
            isLastEntry: Boolean,
            consumerIsSigningUp: Boolean,
        ): LinkAppBarState {
            val showHeaderRoutes = mutableSetOf(
                LinkScreen.Loading.route,
                LinkScreen.SignUp.route,
                LinkScreen.Wallet.route,
                LinkScreen.Verification.route,
            )

            val showEmailRoutes = mutableSetOf(
                LinkScreen.Wallet.route,
            )

            if (consumerIsSigningUp) {
                // If the consumer is just signing up, we show the Link logo and the email address used for signup
                // on the payment method screen.
                showHeaderRoutes.add(LinkScreen.PaymentMethod.route)
                showEmailRoutes.add(LinkScreen.PaymentMethod.route)
            }

            return LinkAppBarState(
                showHeader = route in showHeaderRoutes,
                showOverflowMenu = route == LinkScreen.Wallet.route,
                navigationIcon = if (isLastEntry.not()) {
                    R.drawable.stripe_link_back
                } else {
                    R.drawable.stripe_link_close
                },
                email = email?.takeIf {
                    route in showEmailRoutes
                },
            )
        }
    }
}
