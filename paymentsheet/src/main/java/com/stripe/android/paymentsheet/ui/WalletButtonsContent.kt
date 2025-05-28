package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.LinkButton
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
            interactor.handleViewAction(WalletButtonsInteractor.ViewAction.OnRendered)

            onDispose {
                interactor.handleViewAction(WalletButtonsInteractor.ViewAction.OnUnRendered)
            }
        }

        if (state.walletButtons.isNotEmpty()) {
            StripeTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                        WalletButtonsInteractor.ViewAction.OnButtonPressed(button)
                                    )
                                },
                            )
                            is WalletButtonsInteractor.WalletButton.Link -> LinkButton(
                                email = button.email,
                                enabled = state.buttonsEnabled,
                                onClick = {
                                    interactor.handleViewAction(
                                        WalletButtonsInteractor.ViewAction.OnButtonPressed(button)
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
