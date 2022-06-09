package com.stripe.android.link.ui.verification

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.R
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.ui.core.DefaultPaymentsTheme
import com.stripe.android.ui.core.elements.OTPElement
import com.stripe.android.ui.core.elements.OTPElementUI
import com.stripe.android.ui.core.elements.OTPSpec

@Preview
@Composable
private fun VerificationBodyPreview() {
    DefaultLinkTheme {
        Surface {
            VerificationBody(
                headerStringResId = R.string.verification_header,
                messageStringResId = R.string.verification_message,
                showChangeEmailMessage = true,
                redactedPhoneNumber = "+1********23",
                email = "test@stripe.com",
                otpElement = OTPSpec.transform(),
                isProcessing = false,
                errorMessage = null,
                onBack = { },
                onChangeEmailClick = { },
                onResendCodeClick = { }
            )
        }
    }
}

@Composable
internal fun VerificationBodyFullFlow(
    linkAccount: LinkAccount,
    injector: NonFallbackInjector
) {
    VerificationBody(
        headerStringResId = R.string.verification_header,
        messageStringResId = R.string.verification_message,
        showChangeEmailMessage = true,
        linkAccount = linkAccount,
        injector = injector
    )
}

@Composable
internal fun VerificationBody(
    @StringRes headerStringResId: Int,
    @StringRes messageStringResId: Int,
    showChangeEmailMessage: Boolean,
    linkAccount: LinkAccount,
    injector: NonFallbackInjector,
    onVerificationCompleted: (() -> Unit)? = null
) {
    val viewModel: VerificationViewModel = viewModel(
        factory = VerificationViewModel.Factory(
            linkAccount,
            injector
        )
    )

    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    onVerificationCompleted?.let {
        viewModel.onVerificationCompleted = it
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            focusManager.clearFocus(true)
            keyboardController?.hide()
        }
    }

    VerificationBody(
        headerStringResId = headerStringResId,
        messageStringResId = messageStringResId,
        showChangeEmailMessage = showChangeEmailMessage,
        redactedPhoneNumber = viewModel.linkAccount.redactedPhoneNumber,
        email = viewModel.linkAccount.email,
        otpElement = viewModel.otpElement,
        isProcessing = isProcessing,
        errorMessage = errorMessage,
        onBack = viewModel::onBack,
        onChangeEmailClick = viewModel::onChangeEmailClicked,
        onResendCodeClick = viewModel::startVerification
    )
}

@Composable
internal fun VerificationBody(
    @StringRes headerStringResId: Int,
    @StringRes messageStringResId: Int,
    showChangeEmailMessage: Boolean,
    redactedPhoneNumber: String,
    email: String,
    otpElement: OTPElement,
    isProcessing: Boolean,
    errorMessage: ErrorMessage?,
    onBack: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onResendCodeClick: () -> Unit
) {
    BackHandler(onBack = onBack)

    ScrollableTopLevelColumn {
        Text(
            text = stringResource(headerStringResId),
            modifier = Modifier
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary
        )
        Text(
            text = stringResource(messageStringResId, redactedPhoneNumber),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSecondary
        )
        DefaultPaymentsTheme {
            OTPElementUI(
                enabled = !isProcessing,
                element = otpElement,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
        if (showChangeEmailMessage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.verification_not_email, email),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSecondary
                )
                Text(
                    text = stringResource(id = R.string.verification_change_email),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable(
                            enabled = !isProcessing,
                            onClick = onChangeEmailClick
                        ),
                    style = MaterialTheme.typography.body2
                        .merge(TextStyle(textDecoration = TextDecoration.Underline)),
                    color = MaterialTheme.colors.onSecondary
                )
            }
        }
        errorMessage?.let {
            ErrorText(text = it.getMessage(LocalContext.current.resources))
        }
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.linkColors.componentBorder,
                    shape = MaterialTheme.shapes.small
                )
                .clickable(
                    enabled = !isProcessing,
                    onClick = onResendCodeClick
                ),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentAlpha provides if (isProcessing) ContentAlpha.disabled else ContentAlpha.high,
            ) {
                Text(
                    text = stringResource(id = R.string.verification_resend),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.button,
                    color = MaterialTheme.colors.onPrimary
                        .copy(alpha = LocalContentAlpha.current)
                )
            }
        }
    }
}
