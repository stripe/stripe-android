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
        val showHeader = when (currentRoute) {
            LinkScreen.CardEdit.route -> false
            LinkScreen.PaymentMethod.route -> isRootScreen
            else -> true
        }

        val hideEmail = when (currentRoute) {
            LinkScreen.CardEdit.route,
            LinkScreen.Verification.route,
            LinkScreen.SignUp.route -> true
            LinkScreen.PaymentMethod.route -> !isRootScreen
            else -> false
        }

        val showOverflowMenu = currentRoute == LinkScreen.Wallet.route ||
            (currentRoute == LinkScreen.PaymentMethod.route && isRootScreen)

        LinkAppBarState(
            navigationIcon = if (isRootScreen) {
                R.drawable.ic_link_close
            } else {
                R.drawable.ic_link_back
            },
            showHeader = showHeader,
            showOverflowMenu = showOverflowMenu,
            email = email?.takeUnless { it.isBlank() || hideEmail }
        )
    }
}
