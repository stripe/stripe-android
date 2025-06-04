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
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = LinkTheme.colors.borderDefault,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = LinkTheme.colors.surfacePrimary,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
        ) {
            // Compact verification message
            Text(
                text = "Verify: ${verificationState.redactedPhoneNumber}",
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            // OTP input with minimal padding
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                StripeThemeForLink {
                    OTPElementUI(
                        enabled = !verificationState.isProcessing,
                        element = otpElement,
                        otpInputPlaceholder = " ",
                        middleSpacing = 6.dp,  // Reduced spacing between digits
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

                // Smaller loading indicator
                if (verificationState.isProcessing) {
                    LinkSpinner(
                        modifier = Modifier
                            .size(28.dp)
                            .zIndex(1f),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Compact error message
            verificationState.errorMessage?.let { error ->
                Text(
                    text = error.resolve(LocalContext.current),
                    style = LinkTheme.typography.caption,
                    color = LinkTheme.colors.textCritical,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}
