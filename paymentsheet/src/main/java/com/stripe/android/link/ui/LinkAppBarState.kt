package com.stripe.android.link.ui

import androidx.annotation.DrawableRes
import com.stripe.android.link.LinkScreen
import com.stripe.android.paymentsheet.R

internal data class LinkAppBarState(
    @DrawableRes val navigationIcon: Int,
    val showHeader: Boolean,
    val showOverflowMenu: Boolean,
) {

    internal companion object {

        fun initial(): LinkAppBarState {
            return LinkAppBarState(
                navigationIcon = R.drawable.stripe_link_close,
                showHeader = true,
                showOverflowMenu = false,
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
                navigationIcon = if (previousEntryRoute != null) {
                    R.drawable.stripe_link_back
                } else {
                    R.drawable.stripe_link_close
                },
            )
        }
    }
}
