package com.stripe.android.paymentelement.embedded.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.state.WalletLocation
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.WalletsDivider
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.stripeFormInsets
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
                dividerSpacing = if (screen is EmbeddedNavigator.Screen.VerticalPaymentOptions) {
                    24.dp
                } else {
                    16.dp
                },
            )
        }
    }

    private fun shouldShow(screen: EmbeddedNavigator.Screen): Boolean {
        return when (launchMode) {
            is EmbeddedLaunchMode.Complete -> when (screen) {
                is EmbeddedNavigator.Screen.Form -> !navigator.canGoBack
                is EmbeddedNavigator.Screen.HorizontalPaymentOptions,
                is EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions,
                is EmbeddedNavigator.Screen.VerticalPaymentOptions -> true
                is EmbeddedNavigator.Screen.ManageAll,
                is EmbeddedNavigator.Screen.ManageUpdate -> false
            }
            is EmbeddedLaunchMode.PaymentOptions -> when (screen) {
                is EmbeddedNavigator.Screen.Form -> !navigator.canGoBack
                is EmbeddedNavigator.Screen.HorizontalPaymentOptions -> !navigator.canGoBack
                is EmbeddedNavigator.Screen.VerticalPaymentOptions -> true
                is EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions,
                is EmbeddedNavigator.Screen.ManageAll,
                is EmbeddedNavigator.Screen.ManageUpdate -> false
            }
            is EmbeddedLaunchMode.Form,
            is EmbeddedLaunchMode.Manage -> false
        }
    }
}

@Composable
private fun WalletsHeader(
    state: WalletsState,
    dividerSpacing: Dp,
) {
    if (!state.walletsInHeader) return

    val padding = MaterialTheme.stripeFormInsets.getOuterFormInsets()
    Column(modifier = Modifier.padding(padding)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.wallets(WalletLocation.HEADER).forEach { wallet ->
                when (wallet) {
                    is WalletsState.GooglePay -> GooglePayButton(
                        state = PrimaryButton.State.Ready,
                        allowCreditCards = wallet.allowCreditCards,
                        buttonType = wallet.buttonType,
                        billingAddressParameters = wallet.billingAddressParameters,
                        isEnabled = state.buttonsEnabled,
                        onPressed = state.onGooglePayPressed,
                        cardBrandFilter = state.cardBrandFilter,
                        cardFundingFilter = state.cardFundingFilter,
                        additionalEnabledNetworks = wallet.additionalEnabledNetworks,
                    )
                    is WalletsState.Link -> LinkButton(
                        state = wallet.state,
                        enabled = state.buttonsEnabled,
                        theme = wallet.theme,
                        linkBrand = wallet.linkBrand,
                        onClick = state.onLinkPressed,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.requiredHeight(dividerSpacing))
        WalletsDivider(stringResource(state.dividerTextResource))
    }
}
