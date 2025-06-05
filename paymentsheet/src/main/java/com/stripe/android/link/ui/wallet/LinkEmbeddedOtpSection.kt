package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.LinkIcon
import com.stripe.android.link.ui.LinkSpinner
import com.stripe.android.link.ui.verification.VERIFICATION_OTP_TAG
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementColors
import com.stripe.android.uicore.elements.OTPElementUI

@Composable
internal fun LinkEmbeddedOtpSection(
    verificationState: VerificationViewState,
    otpElement: OTPElement,
    modifier: Modifier = Modifier
) {
    DefaultLinkTheme {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Link logo at the top
            LinkIcon(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(48.dp)
            )

            // Verification instruction message
            Title(verificationState)

            // OTP input
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                StripeThemeForLink {
                    OTPElementUI(
                        enabled = !verificationState.isProcessing,
                        element = otpElement,
                        otpInputPlaceholder = " ",
                        middleSpacing = 6.dp,
                        modifier = Modifier
                            .testTag(VERIFICATION_OTP_TAG)
                            .alpha(if (verificationState.isProcessing) ContentAlpha.disabled else ContentAlpha.high),
                        colors = OTPElementColors(
                            selectedBorder = LinkTheme.colors.borderSelected,
                            placeholder = LinkTheme.colors.textPrimary,
                            background = LinkTheme.colors.surfacePrimary
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

@Composable
private fun Title(verificationState: VerificationViewState) {
    Text(
        text = stringResource(
            R.string.stripe_link_verification_message,
            verificationState.redactedPhoneNumber
        ),
        style = LinkTheme.typography.body,
        color = LinkTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )
}
