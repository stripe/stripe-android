package com.stripe.android.financialconnections.navigation.topappbar

import androidx.annotation.DrawableRes
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.theme.Theme

internal data class TopAppBarState(
    val hideStripeLogo: Boolean = true,
    val forceHideStripeLogo: Boolean = false,
    val allowBackNavigation: Boolean = false,
    val theme: Theme = Theme.default,
    val isTestMode: Boolean = false,
    val allowElevation: Boolean = true,
    val isContentScrolled: Boolean = false,
    val error: Throwable? = null,
    val logoState: TopAppBarLogoState? = null,
) {
    val isElevated: Boolean
        get() = allowElevation && isContentScrolled

    fun apply(update: TopAppBarStateUpdate): TopAppBarState {
        return copy(
            hideStripeLogo = update.hideStripeLogo ?: hideStripeLogo,
            allowBackNavigation = update.allowBackNavigation,
            allowElevation = update.allowElevation,
            error = update.error ?: error,
            forceHideStripeLogo = false,
            logoState = update.logoState ?: logoState,
        )
    }
}

internal data class TopAppBarLogoState(
    val tint: Boolean,
    @DrawableRes val trailingIcon: Int? = null,
    val onClick: (() -> Unit)? = null,
)

internal data class TopAppBarStateUpdate(
    val pane: Pane,
    val allowBackNavigation: Boolean,
    val error: Throwable?,
    val hideStripeLogo: Boolean? = null,
    val allowElevation: Boolean = true,
    val onLogoPress: (() -> Unit)? = null,
    val logoState: TopAppBarLogoState? = null,
)
