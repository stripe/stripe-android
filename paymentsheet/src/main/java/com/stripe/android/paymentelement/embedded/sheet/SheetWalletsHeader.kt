package com.stripe.android.paymentelement.embedded.sheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.ui.WalletsHeader
import javax.inject.Inject

internal class SheetWalletsHeader @Inject constructor(
    private val launchMode: EmbeddedLaunchMode,
    private val navigator: EmbeddedNavigator,
    private val initialPaymentOptionsScreenFactory: InitialPaymentOptionsScreenFactory,
) {
    @Composable
    operator fun invoke(screen: EmbeddedNavigator.Screen) {
        if (!shouldShow(screen)) return

        initialPaymentOptionsScreenFactory.walletsState()?.let { state ->
            WalletsHeader(
                state = state,
                onGooglePayPressed = state.onGooglePayPressed,
                onLinkPressed = state.onLinkPressed,
                dividerSpacing = if (screen is EmbeddedNavigator.Screen.VerticalPaymentOptions) {
                    24.dp
                } else {
                    16.dp
                },
                modifier = Modifier,
                cardBrandFilter = state.cardBrandFilter,
                cardFundingFilter = state.cardFundingFilter,
                additionalContent = {},
            )
        }
    }

    private fun shouldShow(screen: EmbeddedNavigator.Screen): Boolean {
        return when (launchMode) {
            is EmbeddedLaunchMode.PaymentOptions -> when (screen) {
                is EmbeddedNavigator.Screen.Form -> !navigator.canGoBack
                is EmbeddedNavigator.Screen.HorizontalPaymentOptions -> !navigator.canGoBack
                is EmbeddedNavigator.Screen.VerticalPaymentOptions -> true
                is EmbeddedNavigator.Screen.ManageAll,
                is EmbeddedNavigator.Screen.ManageUpdate,
                is EmbeddedNavigator.Screen.SavedPaymentMethodConfirm -> false
            }
            is EmbeddedLaunchMode.Form,
            is EmbeddedLaunchMode.Manage -> false
        }
    }
}
