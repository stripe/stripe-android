package com.stripe.android.link.ui

import androidx.compose.runtime.Composable
import com.stripe.android.link.ui.menus.LinkMenu
import com.stripe.android.link.ui.menus.LinkMenuItem
import com.stripe.android.paymentsheet.R
import com.stripe.android.R as StripeR

internal sealed class LinkLogoutMenuItem(
    override val textResId: Int,
    override val isDestructive: Boolean = false
) : LinkMenuItem {
    data object Logout : LinkLogoutMenuItem(textResId = R.string.stripe_log_out, isDestructive = true)
    data object Cancel : LinkLogoutMenuItem(textResId = StripeR.string.stripe_cancel)
}

@Composable
internal fun LinkLogoutSheet(
    onLogoutClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val items = listOf(
        LinkLogoutMenuItem.Logout,
        LinkLogoutMenuItem.Cancel
    )

    LinkMenu(
        items = items,
        onItemPress = { item ->
            when (item) {
                LinkLogoutMenuItem.Logout -> onLogoutClick()
                LinkLogoutMenuItem.Cancel -> onCancelClick()
            }
        }
    )
}
