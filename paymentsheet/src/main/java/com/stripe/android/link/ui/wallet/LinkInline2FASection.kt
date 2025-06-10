package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.LinkSpinner
import com.stripe.android.link.ui.verification.VERIFICATION_HEADER_IMAGE_TAG
import com.stripe.android.link.ui.verification.VERIFICATION_OTP_TAG
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementColors
import com.stripe.android.uicore.elements.OTPElementUI
import com.stripe.android.uicore.strings.resolve

@Composable
internal fun LinkInline2FASection(
    verificationState: VerificationViewState,
    otpElement: OTPElement,
    modifier: Modifier = Modifier
) {
    DefaultLinkTheme {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = spacedBy(16.dp)
        ) {
            // Link logo at the top
            Image(
                modifier = Modifier
                    .width(48.dp)
                    .testTag(VERIFICATION_HEADER_IMAGE_TAG),
                painter = painterResource(R.drawable.stripe_link_logo),
                contentDescription = stringResource(com.stripe.android.R.string.stripe_link),
            )

            // Verification instruction message
            Title(verificationState)

            // OTP input
            OTPSection(verificationState, otpElement)

            // Compact error message
            verificationState.errorMessage?.let { error ->
                Text(
                    text = error.resolve(),
                    style = LinkTheme.typography.caption,
                    color = LinkTheme.colors.textCritical,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun OTPSection(
    verificationState: VerificationViewState,
    otpElement: OTPElement
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        StripeThemeForLink {
            OTPElementUI(
                enabled = !verificationState.isProcessing,
                element = otpElement,
                otpInputPlaceholder = " ",
                middleSpacing = 8.dp,
                modifier = Modifier
                    .testTag(VERIFICATION_OTP_TAG)
                    .alpha(if (verificationState.isProcessing) ContentAlpha.disabled else ContentAlpha.high),
                colors = OTPElementColors(
                    selectedBorder = LinkTheme.colors.borderSelected,
                    placeholder = LinkTheme.colors.textPrimary,
                    background = LinkTheme.colors.surfaceSecondary,
                    selectedBackground = LinkTheme.colors.surfacePrimary,
                    unselectedBorder = LinkTheme.colors.surfaceSecondary
                ),
            )
        }

        // Smaller loading indicator
        if (verificationState.isProcessing) {
            LinkSpinner(
                modifier = Modifier.size(20.dp),
                strokeWidth = 4.dp,
                filledColor = LinkTheme.colors.buttonPrimary
            )
        }
    }
}

@Composable
private fun Title(
    verificationState: VerificationViewState
) {
    Text(
        text = stringResource(
            R.string.stripe_link_verification_message_short,
            verificationState.redactedPhoneNumber
        ),
        style = LinkTheme.typography.body,
        color = LinkTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(name = "Default state", showBackground = true)
@Composable
private fun LinkEmbeddedOtpSectionDefaultPreview() {
    val otpElement = OTPElement(
        identifier = IdentifierSpec.Generic("otp"),
        controller = OTPController()
    )

    val verificationState = VerificationViewState(
        isProcessing = false,
        requestFocus = true,
        errorMessage = null,
        isSendingNewCode = false,
        didSendNewCode = false,
        redactedPhoneNumber = "***-***-1234",
        email = "user@example.com",
        isDialog = false
    )

    LinkInline2FASection(
        verificationState = verificationState,
        otpElement = otpElement
    )
}

@Preview(name = "Processing state", showBackground = true)
@Composable
private fun LinkEmbeddedOtpSectionProcessingPreview() {
    val otpElement = OTPElement(
        identifier = IdentifierSpec.Generic("otp"),
        controller = OTPController().apply {
            onValueChanged(0, "123456")
        }
    )

    val verificationState = VerificationViewState(
        isProcessing = true,
        requestFocus = false,
        errorMessage = null,
        isSendingNewCode = false,
        didSendNewCode = false,
        redactedPhoneNumber = "***-***-1234",
        email = "user@example.com",
        isDialog = false
    )

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        LinkInline2FASection(
            verificationState = verificationState,
            otpElement = otpElement
        )
    }
}

@Preview(name = "Error state", showBackground = true)
@Composable
private fun LinkEmbeddedOtpSectionErrorPreview() {
    val otpElement = OTPElement(
        identifier = IdentifierSpec.Generic("otp"),
        controller = OTPController().apply {
            onValueChanged(0, "123")
        }
    )

    val verificationState = VerificationViewState(
        isProcessing = false,
        requestFocus = false,
        errorMessage = resolvableString("Invalid verification code. Please try again."),
        isSendingNewCode = false,
        didSendNewCode = false,
        redactedPhoneNumber = "***-***-1234",
        email = "user@example.com",
        isDialog = false
    )

    LinkInline2FASection(
        verificationState = verificationState,
        otpElement = otpElement
    )
}
