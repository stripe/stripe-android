package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.LinkSpinner
import com.stripe.android.link.ui.verification.VERIFICATION_OTP_TAG
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementColors
import com.stripe.android.uicore.elements.OTPElementUI

@Composable
internal fun LinkVerificationSection(
    verificationState: VerificationViewState,
    otpElement: OTPElement,
    modifier: Modifier = Modifier
) {
    DefaultLinkTheme {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = LinkTheme.colors.borderDefault,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    color = LinkTheme.colors.surfacePrimary,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            // Show verification message
            Text(
                text = "Please verify your phone number: ${verificationState.redactedPhoneNumber}",
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // OTP input connected to domain layer with loading overlay
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                StripeThemeForLink {
                    OTPElementUI(
                        enabled = !verificationState.isProcessing,
                        element = otpElement,
                        otpInputPlaceholder = " ",
                        middleSpacing = 8.dp,
                        modifier = Modifier
                            .testTag(VERIFICATION_OTP_TAG)
                            .alpha(if (verificationState.isProcessing) 0.5f else 1f),
                        colors = OTPElementColors(
                            selectedBorder = LinkTheme.colors.borderSelected,
                            placeholder = LinkTheme.colors.textPrimary,
                            background = LinkTheme.colors.surfaceSecondary
                        ),
                    )
                }

                // Loading indicator centered on top of OTP
                if (verificationState.isProcessing) {
                    LinkSpinner(
                        modifier = Modifier
                            .size(36.dp)
                            .zIndex(1f),
                        strokeWidth = 3.dp
                    )
                }
            }

            // Error message if any
            verificationState.errorMessage?.let { error ->
                Text(
                    text = error.resolve(LocalContext.current),
                    style = LinkTheme.typography.body,
                    color = LinkTheme.colors.textCritical,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}
