package com.stripe.android.financialconnections.features.common

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError.Type
import com.stripe.android.financialconnections.ui.LocalTestMode
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.components.TestModeBanner
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.StripeThemeForConnections
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementUI

@Composable
internal fun VerificationSection(
    focusRequester: FocusRequester,
    otpElement: OTPElement,
    enabled: Boolean,
    confirmVerificationError: Throwable?,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Column(modifier) {
        StripeThemeForConnections {
            if (LocalTestMode.current) {
                TestModeBanner(
                    enabled = enabled,
                    buttonLabel = stringResource(R.string.stripe_verification_useTestCode),
                    onButtonClick = otpElement::populateTestCode,
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            OTPElementUI(
                otpInputPlaceholder = "",
                boxSpacing = 8.dp,
                middleSpacing = 8.dp,
                boxTextStyle = typography.headingXLargeSubdued.copy(
                    color = colors.textDefault,
                    textAlign = TextAlign.Center
                ),
                focusRequester = focusRequester,
                enabled = enabled,
                element = otpElement
            )
        }
        LaunchedEffect(confirmVerificationError) {
            if (confirmVerificationError is OTPError) {
                if (SDK_INT >= VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                }
            }
        }
        if (confirmVerificationError is OTPError) {
            Spacer(modifier = Modifier.size(16.dp))
            VerificationErrorText(
                error = confirmVerificationError,
            )
        }
    }
}

/**
 * Error text to show under verification inputs in forms.
 */
@Composable
private fun VerificationErrorText(
    error: OTPError,
) {
    val uriHandler = LocalUriHandler.current
    AnnotatedText(
        modifier = Modifier.fillMaxWidth(),
        text = error.toMessage(),
        defaultStyle = typography.labelMedium.copy(
            color = colors.textCritical,
            textAlign = TextAlign.Center
        ),
        onClickableTextClick = {
            uriHandler.openUri(error.supportUrl)
        },
        annotationStyles = mapOf(
            StringAnnotation.CLICKABLE to typography.labelMedium.copy(
                color = colors.textCritical,
                textDecoration = TextDecoration.Underline,
                textAlign = TextAlign.Center
            ).toSpanStyle()
        ),
    )
}

private fun OTPError.toMessage(): TextResource = TextResource.StringId(
    when (type) {
        Type.EMAIL_CODE_EXPIRED -> R.string.stripe_verification_codeExpiredEmail
        Type.SMS_CODE_EXPIRED -> R.string.stripe_verification_codeExpiredSms
        Type.CODE_INVALID -> R.string.stripe_verification_codeInvalid
    }
)

private fun OTPElement.populateTestCode() {
    for (character in "000000") {
        controller.onAutofillDigit(character.toString())
    }
}
