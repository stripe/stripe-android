package com.stripe.android.financialconnections.navigation.topappbar

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane

internal data class TopAppBarState(
    val hideStripeLogo: Boolean = true,
    val forceHideStripeLogo: Boolean = false,
    val allowBackNavigation: Boolean = false,
    val isTestMode: Boolean = false,
    val allowElevation: Boolean = true,
    val isContentScrolled: Boolean = false,
) {

    val isElevated: Boolean
        get() = allowElevation && isContentScrolled

    fun apply(update: TopAppBarStateUpdate): TopAppBarState {
        return copy(
            hideStripeLogo = update.hideStripeLogo ?: hideStripeLogo,
            allowBackNavigation = update.allowBackNavigation,
            allowElevation = update.allowElevation,
            forceHideStripeLogo = false,
        )
    }
}

// TODO(tillh-stripe) Consider `pendingError` or similar for account picker screen
internal data class TopAppBarStateUpdate(
    val pane: Pane,
    val hideStripeLogo: Boolean? = null,
    val allowBackNavigation: Boolean,
    val allowElevation: Boolean = true,
)
