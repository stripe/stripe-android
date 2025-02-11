package com.stripe.android.link.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.menu.LinkMenu
import com.stripe.android.link.ui.menu.LinkMenuItem
import com.stripe.android.paymentsheet.R

@Composable
internal fun LinkAppBarMenu(
    onLogoutClicked: () -> Unit
) {
    val items = remember {
        listOf(LogoutMenuItem)
    }

    LinkMenu(
        items = items
    ) {
        onLogoutClicked()
    }
}

internal data object LogoutMenuItem : LinkMenuItem {
    override val text = R.string.stripe_log_out.resolvableString
    override val testTag = LOGOUT_MENU_ROW_TAG
    override val isDestructive = true
}

internal const val LOGOUT_MENU_ROW_TAG = "logout_menu_row_tag"
