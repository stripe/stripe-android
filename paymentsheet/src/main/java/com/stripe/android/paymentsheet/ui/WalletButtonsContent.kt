package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.link.verification.LinkEmbeddedState
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
        
        // Track whether we're initially loading Link state
        var isLoadingLinkState by remember { mutableStateOf(true) }
        var previousVerificationState by remember { mutableStateOf<LinkEmbeddedState?>(null) }
        
        // Detect changes to Link verification state
        LaunchedEffect(state.linkEmbeddedState) {
            // After first load or if verification state changes, we're no longer loading
            if (previousVerificationState != null || state.linkEmbeddedState == null) {
                isLoadingLinkState = false
            }
            previousVerificationState = state.linkEmbeddedState
        }

        if (state.walletButtons.isNotEmpty()) {
            StripeTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Find Link button and other wallet buttons
                    val linkButton = state.walletButtons.find { it is WalletButtonsInteractor.WalletButton.Link } as? WalletButtonsInteractor.WalletButton.Link
                    val otherWalletButtons = state.walletButtons.filterIsInstance<WalletButtonsInteractor.WalletButton.GooglePay>()
                    
                    // First check if 2FA is required
                    val needsVerification = linkButton != null && 
                                           state.linkEmbeddedState?.verificationState != null && 
                                           state.otpElement != null
                    
                    if (needsVerification) {
                        // Show 2FA verification section at the top but prevent keyboard focus
                        DefaultLinkTheme {
                            // Create a custom FocusRequester that we don't focus initially
                            val focusRequester = remember { FocusRequester() }
                            
                            // Pass the LinkVerificationSection with our custom OTPElementUI implementation
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Please verify your phone number: ${state.linkEmbeddedState!!.verificationState!!.redactedPhoneNumber}",
                                    style = LinkTheme.typography.caption,
                                    color = LinkTheme.colors.textPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                )
                                
                                StripeThemeForLink {
                                    OTPElementUI(
                                        enabled = !state.linkEmbeddedState!!.verificationState!!.isProcessing,
                                        element = state.otpElement!!,
                                        otpInputPlaceholder = " ",
                                        middleSpacing = 8.dp,
                                        focusRequester = focusRequester, // Use our non-autofocusing requester
                                        modifier = Modifier
                                            .padding(vertical = 10.dp),
                                        colors = OTPElementColors(
                                            selectedBorder = LinkTheme.colors.borderSelected,
                                            placeholder = LinkTheme.colors.textPrimary,
                                            background = LinkTheme.colors.surfaceSecondary
                                        ),
                                    )
                                }
                                
                                // Error message if any
                                state.linkEmbeddedState!!.verificationState!!.errorMessage?.let { error ->
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
                                if (state.linkEmbeddedState!!.verificationState!!.isProcessing) {
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
                        
                        // Add divider with "or pay with" text if we have other wallet buttons
                        if (otherWalletButtons.isNotEmpty()) {
                            WalletsDivider(text = "or pay with")
                        }
                    }
                    
                    // Show loading indicator if we're waiting on Link state
                    if (isLoadingLinkState && linkButton != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colors.primary
                            )
                        }
                    } else {
                        // Now render the wallet buttons (excluding Link if 2FA is needed)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Show the Link button only if no verification is needed
                            if (linkButton != null && !needsVerification) {
                                LinkButton(
                                    email = linkButton.email,
                                    enabled = state.buttonsEnabled,
                                    onClick = {
                                        interactor.handleViewAction(
                                            WalletButtonsInteractor.ViewAction.OnButtonPressed(linkButton)
                                        )
                                    }
                                )
                            }
                            
                            // Show the other wallet buttons
                            otherWalletButtons.forEach { button ->
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
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
