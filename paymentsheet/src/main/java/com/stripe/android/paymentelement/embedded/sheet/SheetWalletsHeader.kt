package com.stripe.android.paymentelement.embedded.sheet

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.navigation.horizontalModeWalletsDividerSpacing
import com.stripe.android.paymentsheet.navigation.verticalModeWalletsDividerSpacing
import com.stripe.android.paymentsheet.state.WalletLocation
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.WalletsHeader
import javax.inject.Inject

internal class SheetWalletsHeader @Inject constructor(
    private val launchMode: EmbeddedLaunchMode,
    private val navigator: EmbeddedNavigator,
    private val initialPaymentOptionsScreenFactory: InitialPaymentOptionsScreenFactory,
) {
    fun stateFor(screen: EmbeddedNavigator.Screen): State? {
        val dividerSpacing = when (screen) {
            is EmbeddedNavigator.Screen.Form,
            is EmbeddedNavigator.Screen.HorizontalPaymentOptions -> horizontalModeWalletsDividerSpacing
            is EmbeddedNavigator.Screen.VerticalPaymentOptions -> verticalModeWalletsDividerSpacing
            is EmbeddedNavigator.Screen.ManageAll,
            is EmbeddedNavigator.Screen.ManageUpdate,
            is EmbeddedNavigator.Screen.SavedPaymentMethodConfirm -> return null
        }
        val walletsState = visibleWalletsState() ?: return null

        return State(
            walletsState = walletsState,
            dividerSpacing = dividerSpacing,
        )
    }

    fun isVisible(): Boolean {
        return visibleWalletsState() != null
    }

    private fun visibleWalletsState(): WalletsState? {
        val shouldShowHeader = shouldShowWalletsHeader(
            launchMode = launchMode,
            canGoBack = navigator.canGoBack,
        )
        if (!shouldShowHeader) return null

        val walletsState = initialPaymentOptionsScreenFactory.walletsState() ?: return null
        if (walletsState.wallets(WalletLocation.HEADER).isEmpty()) return null

        return walletsState
    }

    data class State(
        val walletsState: WalletsState,
        val dividerSpacing: Dp,
    )
}

internal fun shouldShowWalletsHeader(
    launchMode: EmbeddedLaunchMode,
    canGoBack: Boolean,
): Boolean {
    return launchMode is EmbeddedLaunchMode.PaymentOptions && !canGoBack
}

@Composable
internal fun SheetWalletsHeaderContent(
    state: WalletsState,
    dividerSpacing: Dp,
) {
    WalletsHeader(
        state = state,
        onGooglePayPressed = state.onGooglePayPressed,
        onLinkPressed = state.onLinkPressed,
        dividerSpacing = dividerSpacing,
        modifier = Modifier.padding(bottom = dividerSpacing),
        cardBrandFilter = state.cardBrandFilter,
        cardFundingFilter = state.cardFundingFilter,
        additionalContent = null,
    )
}
