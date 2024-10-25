package com.stripe.android.link.ui

import androidx.annotation.DrawableRes

internal data class LinkAppBarState(
    @DrawableRes val navigationIcon: Int,
    val showHeader: Boolean,
    val showOverflowMenu: Boolean,
    val email: String?,
)
