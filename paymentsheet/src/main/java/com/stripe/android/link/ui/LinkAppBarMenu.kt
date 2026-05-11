package com.stripe.android.link.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.menu.LinkMenu
import com.stripe.android.link.ui.menu.LinkMenuItem
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R

@Composable
internal fun LinkAppBarMenu(
    linkBrand: LinkBrand,
    onLogoutClicked: () -> Unit
) {
    val items = remember(linkBrand) {
        listOf(LogoutMenuItem(linkBrand))
    }

    LinkMenu(
        items = items
    ) {
        onLogoutClicked()
    }
}

internal data class LogoutMenuItem(
    private val linkBrand: LinkBrand,
) : LinkMenuItem {
    override val text = if (linkBrand == LinkBrand.Link) {
        R.string.stripe_log_out.resolvableString
    } else {
        resolvableString(R.string.stripe_log_out_with_brand, linkBrand.brandName())
    }
    override val testTag = LOGOUT_MENU_ROW_TAG
    override val isDestructive = true
}

internal const val LOGOUT_MENU_ROW_TAG = "logout_menu_row_tag"
