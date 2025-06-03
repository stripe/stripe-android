package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.OTPElementColors
import com.stripe.android.uicore.elements.OTPElementUI
import com.stripe.android.uicore.utils.collectAsState

@Immutable
internal class WalletButtonsContent(
    private val interactor: WalletButtonsInteractor,
) {
    @Composable
    fun Content() {
        val state by interactor.state.collectAsState()
        
        if (state.walletButtons.isEmpty()) return
        
        StripeTheme {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Render the appropriate buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.walletButtons.forEach { button ->
                        when (button) {
                            is WalletButtonsInteractor.WalletButton.Link2FA -> {
                                // Render Link 2FA verification UI
                                LinkVerificationUI(button = button)
                                if (state.walletButtons.size > 1) {
                                    WalletsDivider(text = "or pay with")
                                }
                            }
                            
                            is WalletButtonsInteractor.WalletButton.Link -> {
                                LinkButton(
                                    email = button.email,
                                    enabled = state.buttonsEnabled,
                                    onClick = {
                                        interactor.handleViewAction(
                                            WalletButtonsInteractor.ViewAction.OnButtonPressed(button)
                                        )
                                    }
                                )
                            }
                            
                            is WalletButtonsInteractor.WalletButton.GooglePay -> {
                                GooglePayButton(
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
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun LinkVerificationUI(
        button: WalletButtonsInteractor.WalletButton.Link2FA
    ) {
        DefaultLinkTheme {
            // Create a non-focusing requester to prevent keyboard from showing automatically
            val focusRequester = remember { FocusRequester() }
            val verificationViewState = button.verificationViewState
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Please verify your phone number: ${verificationViewState.redactedPhoneNumber}",
                    style = LinkTheme.typography.caption,
                    color = LinkTheme.colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                
                StripeThemeForLink {
                    OTPElementUI(
                        enabled = !verificationViewState.isProcessing,
                        element = button.otpElement,
                        otpInputPlaceholder = " ",
                        middleSpacing = 8.dp,
                        focusRequester = focusRequester, // Don't auto-focus
                        modifier = Modifier.padding(vertical = 10.dp),
                        colors = OTPElementColors(
                            selectedBorder = LinkTheme.colors.borderSelected,
                            placeholder = LinkTheme.colors.textPrimary,
                            background = LinkTheme.colors.surfaceSecondary
                        )
                    )
                }
                
                // Error message
                verificationViewState.errorMessage?.let { error ->
                    Text(
                        text = error.resolve(LocalContext.current),
                        style = LinkTheme.typography.body,
                        color = LinkTheme.colors.textCritical,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
                
                // Processing indicator
                if (verificationViewState.isProcessing) {
                    Text(
                        text = "Verifying...",
                        style = LinkTheme.typography.body,
                        color = LinkTheme.colors.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }
}