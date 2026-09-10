package com.stripe.android.paymentelement.embedded.sheet

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.state.WalletLocation
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.WalletsHeader
import javax.inject.Inject

internal class SheetWalletsHeader @Inject constructor(
    private val launchMode: EmbeddedLaunchMode,
    private val navigator: EmbeddedNavigator,
    private val initialPaymentOptionsScreenFactory: InitialPaymentOptionsScreenFactory,
) {
    @Composable
    operator fun invoke(state: State) {
        SheetWalletsHeaderContent(
            state = state.walletsState,
            dividerSpacing = state.dividerSpacing,
        )
    }

    fun stateFor(screen: EmbeddedNavigator.Screen): State? {
        val shouldShowHeader = shouldShowWalletsHeader(
            launchMode = launchMode,
            behavior = screen.walletsHeaderBehavior,
            canGoBack = navigator.canGoBack,
        )
        if (!shouldShowHeader) return null

        val walletsState = initialPaymentOptionsScreenFactory.walletsState() ?: return null
        if (walletsState.wallets(WalletLocation.HEADER).isEmpty()) return null

        return State(
            walletsState = walletsState,
            dividerSpacing = screen.walletsDividerSpacing,
        )
    }

    data class State(
        val walletsState: WalletsState,
        val dividerSpacing: Dp,
    )
}

internal fun shouldShowWalletsHeader(
    launchMode: EmbeddedLaunchMode,
    behavior: EmbeddedNavigator.WalletsHeaderBehavior,
    canGoBack: Boolean,
): Boolean {
    return launchMode is EmbeddedLaunchMode.PaymentOptions && behavior.isVisible(canGoBack)
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
        additionalContent = {},
    )
}
