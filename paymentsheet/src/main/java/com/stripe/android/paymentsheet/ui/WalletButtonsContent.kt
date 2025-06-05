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
import com.stripe.android.link.ui.wallet.LinkEmbeddedOtpSection
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

        if (state.walletButtons.isNotEmpty() || state.linkOtpState != null) {
            StripeTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.linkOtpState?.let {
                        // Render Link 2FA verification UI
                        LinkEmbeddedOtpSection(
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
