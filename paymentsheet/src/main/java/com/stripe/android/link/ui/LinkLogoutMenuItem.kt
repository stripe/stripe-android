package com.stripe.android.link.ui

import com.stripe.android.link.ui.menus.LinkMenuItem
import com.stripe.android.paymentsheet.R

internal sealed class LinkLogoutMenuItem(
    override val textResId: Int,
    override val isDestructive: Boolean = false
) : LinkMenuItem {
    data object Logout : LinkLogoutMenuItem(textResId = R.string.stripe_log_out, isDestructive = true)
    data object Cancel : LinkLogoutMenuItem(textResId = com.stripe.android.R.string.stripe_cancel)
}
