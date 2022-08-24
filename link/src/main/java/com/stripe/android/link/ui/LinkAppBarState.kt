package com.stripe.android.link.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.R

internal data class LinkAppBarState(
    @DrawableRes val navigationIcon: Int,
    val showHeader: Boolean,
    val showOverflowMenu: Boolean,
    val email: String?
)

@Composable
internal fun rememberLinkAppBarState(
    isRootScreen: Boolean,
    currentRoute: String?,
    email: String?
): LinkAppBarState {
    return remember(currentRoute, email) {
        val routesWithoutHeader = setOf(
            LinkScreen.CardEdit.route,
            LinkScreen.PaymentMethod.route
        )

        val routesWithoutEmail = setOf(
            LinkScreen.Verification.route,
            LinkScreen.SignUp.route
        ) + routesWithoutHeader

        val hideHeader = currentRoute in routesWithoutHeader
        val hideEmail = email.isNullOrBlank() || currentRoute in routesWithoutEmail

        val showOverflowMenu = currentRoute == LinkScreen.Wallet.route

        LinkAppBarState(
            navigationIcon = if (isRootScreen) {
                R.drawable.ic_link_close
            } else {
                R.drawable.ic_link_back
            },
            showHeader = !hideHeader,
            showOverflowMenu = showOverflowMenu,
            email = email?.takeIf { it.isNotBlank() && !hideEmail }
        )
    }
}
