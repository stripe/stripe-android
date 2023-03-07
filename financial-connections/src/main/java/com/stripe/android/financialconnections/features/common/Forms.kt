package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
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


internal enum class VerificationFormState {
    OPEN,
    VERIFYING,
    VERIFIED,
    ERROR_CODE_INVALID,
    ERROR_CODE_EXPIRED,
    ERROR_MAX_ATTEMPTS_EXCEEDED,
    UNKNOWN_ERROR,
}

@Composable
internal fun VerificationErrorMessage(
    formState: VerificationFormState,
    otpType: VerificationType = VerificationType.SMS
) {
    when (formState) {
        VerificationFormState.ERROR_CODE_INVALID -> {
            AnnotatedText(
                text = TextResource.StringId(R.string.stripe_verification_codeInvalid),
                onClickableTextClick = { TODO() },
                defaultStyle = FinancialConnectionsTheme.typography.body,
            )
        }

        VerificationFormState.ERROR_CODE_EXPIRED,
        VerificationFormState.ERROR_MAX_ATTEMPTS_EXCEEDED -> {
            AnnotatedText(
                text = TextResource.StringId(
                    when (otpType) {
                        VerificationType.EMAIL -> R.string.stripe_verification_codeExpiredEmail
                        VerificationType.SMS -> R.string.stripe_verification_codeExpiredSms
                    },
                    listOf("https://support.link.co/contact/email?skipVerification=true")
                ),
                onClickableTextClick = { TODO() },
                defaultStyle = FinancialConnectionsTheme.typography.body,
            )
        }

        VerificationFormState.UNKNOWN_ERROR -> AnnotatedText(
            text = TextResource.StringId(
                R.string.stripe_verification_unexpectedError
            ),
            onClickableTextClick = { TODO() },
            defaultStyle = FinancialConnectionsTheme.typography.body,
        )

        VerificationFormState.OPEN,
        VerificationFormState.VERIFYING,
        VerificationFormState.VERIFIED -> {
            Box(modifier = Modifier.padding(0.dp))
        }
    }
}