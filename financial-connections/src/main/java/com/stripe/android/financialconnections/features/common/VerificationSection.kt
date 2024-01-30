package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError.Type
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.financialconnections.ui.theme.StripeThemeForConnections
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementUI

@Composable
internal fun VerificationSection(
    focusRequester: FocusRequester,
    otpElement: OTPElement,
    enabled: Boolean,
    confirmVerificationError: Throwable?,
) {
    Column {
        StripeThemeForConnections {
            OTPElementUI(
                otpInputPlaceholder = "",
                boxSpacing = 8.dp,
                middleSpacing = 8.dp,
                boxTextStyle = v3Typography.headingXLargeSubdued.copy(
                    color = v3Colors.textDefault,
                    textAlign = TextAlign.Center
                ),
                focusRequester = focusRequester,
                enabled = enabled,
                element = otpElement
            )
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
        defaultStyle = v3Typography.labelMedium.copy(
            color = v3Colors.textCritical,
            textAlign = TextAlign.Center
        ),
        onClickableTextClick = {
            uriHandler.openUri(FinancialConnectionsUrlResolver.linkVerificationSupportUrl)
        },
        annotationStyles = mapOf(
            StringAnnotation.CLICKABLE to v3Typography.labelMedium.copy(
                color = v3Colors.textCritical,
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
