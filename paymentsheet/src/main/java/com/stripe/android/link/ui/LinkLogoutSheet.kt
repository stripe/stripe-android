package com.stripe.android.link.ui

import androidx.compose.runtime.Composable
import com.stripe.android.link.ui.menus.LinkMenu

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
