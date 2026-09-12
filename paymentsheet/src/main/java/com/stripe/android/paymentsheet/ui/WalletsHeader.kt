package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.paymentsheet.state.WalletLocation
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.stripeFormInsets

@Composable
internal fun WalletsHeader(
    state: WalletsState,
    onGooglePayPressed: () -> Unit,
    onLinkPressed: () -> Unit,
    dividerSpacing: Dp,
    modifier: Modifier,
    cardBrandFilter: CardBrandFilter,
    cardFundingFilter: CardFundingFilter,
    additionalContent: @Composable () -> Unit,
) {
    val walletItems = remember(state) {
        state.wallets(WalletLocation.HEADER)
    }
    if (walletItems.isEmpty()) return

    val padding = MaterialTheme.stripeFormInsets.getOuterFormInsets()
    Column(modifier = modifier.padding(padding)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            walletItems.forEach { wallet ->
                when (wallet) {
                    is WalletsState.GooglePay -> GooglePayButton(
                        state = PrimaryButton.State.Ready,
                        allowCreditCards = wallet.allowCreditCards,
                        buttonType = wallet.buttonType,
                        billingAddressParameters = wallet.billingAddressParameters,
                        isEnabled = state.buttonsEnabled,
                        onPressed = onGooglePayPressed,
                        cardBrandFilter = cardBrandFilter,
                        cardFundingFilter = cardFundingFilter,
                        additionalEnabledNetworks = wallet.additionalEnabledNetworks,
                    )
                    is WalletsState.Link -> LinkButton(
                        state = wallet.state,
                        enabled = state.buttonsEnabled,
                        theme = wallet.theme,
                        linkBrand = wallet.linkBrand,
                        onClick = onLinkPressed,
                    )
                }
            }
        }

        additionalContent()

        Spacer(modifier = Modifier.requiredHeight(dividerSpacing))
        WalletsDivider(stringResource(state.dividerTextResource))
    }
}
