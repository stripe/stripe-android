package com.stripe.android.link.ui

import androidx.compose.runtime.compositionLocalOf

internal val LocalLinkContentScrollHandler = compositionLocalOf<LinkContentScrollHandler?> { null }

internal class LinkContentScrollHandler(
    private val onCanScrollBackwardChanged: (Boolean) -> Unit,
) {

    fun handleCanScrollBackwardChanged(canScrollBackward: Boolean) {
        onCanScrollBackwardChanged(canScrollBackward)
    }
}
