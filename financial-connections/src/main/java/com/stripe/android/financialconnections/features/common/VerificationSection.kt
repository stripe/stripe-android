package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError.Type
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
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
                focusRequester = focusRequester,
                enabled = enabled,
                element = otpElement
            )
        }
        if (confirmVerificationError is OTPError) {
            Spacer(modifier = Modifier.size(4.dp))
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
    Row {
        Icon(
            modifier = Modifier
                .size(12.dp)
                .offset(y = 2.dp),
            painter = painterResource(R.drawable.stripe_ic_warning),
            contentDescription = "Warning icon",
            tint = FinancialConnectionsTheme.colors.textCritical
        )
        AnnotatedText(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = error.toMessage(),
            defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
                color = FinancialConnectionsTheme.colors.textCritical
            ),
            onClickableTextClick = {
                uriHandler.openUri(FinancialConnectionsUrlResolver.linkVerificationSupportUrl)
            },
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.caption.copy(
                    color = FinancialConnectionsTheme.colors.textCritical,
                    textDecoration = TextDecoration.Underline
                ).toSpanStyle()
            ),
        )
    }
}

private fun OTPError.toMessage(): TextResource = TextResource.StringId(
    when (type) {
        Type.EMAIL_CODE_EXPIRED -> R.string.stripe_verification_codeExpiredEmail
        Type.SMS_CODE_EXPIRED -> R.string.stripe_verification_codeExpiredSms
        Type.CODE_INVALID -> R.string.stripe_verification_codeInvalid
    }
)
