package com.stripe.android.link.ui

import androidx.compose.runtime.Composable
import com.stripe.android.link.R
import com.stripe.android.link.ui.menus.LinkMenu
import com.stripe.android.link.ui.menus.LinkMenuItem

internal sealed class LinkLogoutMenuItem(
    override val textResId: Int,
    override val isDestructive: Boolean = false
) : LinkMenuItem {
    object Logout : LinkLogoutMenuItem(textResId = R.string.log_out, isDestructive = true)
    object Cancel : LinkLogoutMenuItem(textResId = R.string.cancel)
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
