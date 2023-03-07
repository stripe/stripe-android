package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.model.VerificationType

/**
 * Error text to show under form elements.
 */
@Composable
internal fun FormErrorText(error: Throwable) {
    Text(
        modifier = Modifier.padding(horizontal = 4.dp),
        text = error.localizedMessage ?: stringResource(id = R.string.stripe_error_generic_title),
        color = FinancialConnectionsTheme.colors.textCritical,
        style = FinancialConnectionsTheme.typography.caption
    )
}

/**
 * Error text to show under verification inputs in forms.
 */
@Composable
fun VerificationErrorText(error: Throwable, verificationType: VerificationType) {
    val uriHandler = LocalUriHandler.current
    AnnotatedText(
        modifier = Modifier.padding(horizontal = 4.dp),
        text = getVerificationErrorMessage(error = error, verificationType = verificationType),
        defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
            color = FinancialConnectionsTheme.colors.textCritical
        ),
        onClickableTextClick = { uriHandler.openUri("https://support.link.co/contact/email?skipVerification=true") },
        annotationStyles = mapOf(
            StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.caption.copy(
                color = FinancialConnectionsTheme.colors.textCritical,
                textDecoration = TextDecoration.Underline
            ).toSpanStyle()
        ),
    )
}

@Composable
private fun getVerificationErrorMessage(
    error: Throwable,
    verificationType: VerificationType
): TextResource {
    return when ((error as StripeException).stripeError?.code ?: "") {
        "consumer_verification_code_invalid" -> TextResource.StringId(R.string.stripe_verification_codeInvalid)
        "consumer_session_expired",
        "consumer_verification_expired",
        "consumer_verification_max_attempts_exceeded" -> TextResource.StringId(
            when (verificationType) {
                VerificationType.EMAIL -> R.string.stripe_verification_codeExpiredEmail
                VerificationType.SMS -> R.string.stripe_verification_codeExpiredSms
            },
            listOf("https://support.link.co/contact/email?skipVerification=true")
        )

        else -> TextResource.StringId(
            R.string.stripe_verification_unexpectedError
        )
    }
}
