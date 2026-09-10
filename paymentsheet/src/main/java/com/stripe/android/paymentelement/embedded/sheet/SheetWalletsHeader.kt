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

internal fun walletsHeaderState(
    launchMode: EmbeddedLaunchMode,
    screen: EmbeddedNavigator.Screen,
    canGoBack: Boolean,
    walletsState: WalletsState?,
): SheetWalletsHeaderState? {
    if (launchMode !is EmbeddedLaunchMode.PaymentOptions || canGoBack) return null
    if (walletsState == null || walletsState.wallets(WalletLocation.HEADER).isEmpty()) return null

    val dividerSpacing = when (screen) {
        is EmbeddedNavigator.Screen.Form,
        is EmbeddedNavigator.Screen.VerticalPaymentOptions -> verticalModeWalletsDividerSpacing
        is EmbeddedNavigator.Screen.HorizontalPaymentOptions,
        is EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions -> horizontalModeWalletsDividerSpacing
        is EmbeddedNavigator.Screen.ManageAll,
        is EmbeddedNavigator.Screen.ManageUpdate,
        is EmbeddedNavigator.Screen.SavedPaymentMethodConfirm -> return null
    }
    return SheetWalletsHeaderState(walletsState, dividerSpacing)
}

internal data class SheetWalletsHeaderState(
    val walletsState: WalletsState,
    val dividerSpacing: Dp,
)

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
