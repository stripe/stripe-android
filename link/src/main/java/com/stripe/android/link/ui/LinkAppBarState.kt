package com.stripe.android.link.ui

import androidx.annotation.DrawableRes
import com.stripe.android.link.model.AccountStatus

internal data class LinkAppBarState(
    @DrawableRes val navigationIcon: Int,
    val showHeader: Boolean,
    val showOverflowMenu: Boolean,
    val email: String?,
)
