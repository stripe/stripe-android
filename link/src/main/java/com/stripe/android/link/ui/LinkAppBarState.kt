package com.stripe.android.link.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.R
import com.stripe.android.link.model.AccountStatus

internal data class LinkAppBarState(
    @DrawableRes val navigationIcon: Int,
    val showHeader: Boolean,
    val showOverflowMenu: Boolean,
    val email: String?,
    val accountStatus: AccountStatus?
)

@Composable
internal fun rememberLinkAppBarState(
    isRootScreen: Boolean,
    currentRoute: String?,
    email: String?,
    accountStatus: AccountStatus?
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

        // If there's an email address, we want to allow the user to log
        // out of the existing account.
        val showOverflowMenu = isRootScreen && email != null && accountStatus == AccountStatus.Verified

        LinkAppBarState(
            navigationIcon = if (isRootScreen) {
                R.drawable.stripe_link_close
            } else {
                R.drawable.stripe_link_back
            },
            showHeader = showHeader,
            showOverflowMenu = showOverflowMenu,
            email = email?.takeUnless { it.isBlank() || hideEmail },
            accountStatus = accountStatus
        )
    }
}
