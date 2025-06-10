package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.link.ui.wallet.LinkInline2FASection
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor.ViewAction.OnButtonPressed
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.utils.collectAsState

@Immutable
internal class WalletButtonsContent(
    private val interactor: WalletButtonsInteractor,
) {
    @Composable
    fun Content() {
        val state by interactor.state.collectAsState()

        DisposableEffect(Unit) {
            interactor.handleViewAction(WalletButtonsInteractor.ViewAction.OnShown)

            onDispose {
                interactor.handleViewAction(WalletButtonsInteractor.ViewAction.OnHidden)
            }
        }

        // Render the wallet buttons and 2FA section if they exist
        if (state.hasContent) {
            StripeTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Render the Link 2FA section verification is in progress
                    state.link2FAState?.let {
                        LinkInline2FASection(
                            verificationState = it.viewState,
                            otpElement = it.otpElement
                        )
                        if (state.walletButtons.size > 1) {
                            WalletsDivider(stringResource(R.string.stripe_paymentsheet_or_use))
                        }
                    }

                    state.walletButtons.forEach { button ->
                        when (button) {
                            is WalletButtonsInteractor.WalletButton.GooglePay -> GooglePayButton(
                                state = PrimaryButton.State.Ready,
                                allowCreditCards = button.allowCreditCards,
                                buttonType = button.googlePayButtonType,
                                billingAddressParameters = button.billingAddressParameters,
                                isEnabled = state.buttonsEnabled,
                                cardBrandFilter = button.cardBrandFilter,
                                onPressed = {
                                    interactor.handleViewAction(
                                        OnButtonPressed(button)
                                    )
                                },
                            )
                            // Link button is filtered out if the 2FA verification is in progress
                            is WalletButtonsInteractor.WalletButton.Link -> LinkButton(
                                email = button.email,
                                enabled = state.buttonsEnabled,
                                onClick = {
                                    interactor.handleViewAction(
                                        OnButtonPressed(button)
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
