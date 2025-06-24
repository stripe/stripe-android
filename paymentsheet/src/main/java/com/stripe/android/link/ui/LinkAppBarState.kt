package com.stripe.android.link.ui

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkScreen
import com.stripe.android.paymentsheet.R

internal data class LinkAppBarState(
    val showHeader: Boolean,
    val canNavigateBack: Boolean,
    val title: ResolvableString?,
) {

    val canShowCloseIcon: Boolean
        get() = !canNavigateBack

    internal companion object {

        fun initial(): LinkAppBarState {
            return LinkAppBarState(
                showHeader = true,
                canNavigateBack = false,
                title = null,
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

            val title = when (route) {
                LinkScreen.PaymentMethod.route -> R.string.stripe_add_payment_method.resolvableString
                LinkScreen.UpdateCard.route -> R.string.stripe_link_update_card_title.resolvableString
                else -> null
            }

            return LinkAppBarState(
                showHeader = route in showHeaderRoutes,
                canNavigateBack = previousEntryRoute != null,
                title = title,
            )
        }
    }
}
