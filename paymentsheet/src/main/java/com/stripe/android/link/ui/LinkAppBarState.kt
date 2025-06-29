package com.stripe.android.link.ui

import androidx.navigation.NavBackStackEntry
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.LinkScreen.Companion.billingDetailsUpdateFlow
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
            currentEntry: NavBackStackEntry?,
            previousEntryRoute: String?,
            consumerIsSigningUp: Boolean,
        ): LinkAppBarState {
            val route = currentEntry?.destination?.route
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
                LinkScreen.UpdateCard.route -> updateCardTitle(currentEntry)
                else -> null
            }

            return LinkAppBarState(
                showHeader = route in showHeaderRoutes,
                canNavigateBack = previousEntryRoute != null,
                title = title,
            )
        }

        private fun updateCardTitle(currentEntry: NavBackStackEntry): ResolvableString {
            return if (currentEntry.billingDetailsUpdateFlow() != null) {
                R.string.stripe_link_confirm_payment_title.resolvableString
            } else {
                R.string.stripe_link_update_card_title.resolvableString
            }
        }
    }
}
