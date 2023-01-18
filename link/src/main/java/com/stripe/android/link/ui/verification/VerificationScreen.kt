package com.stripe.android.link.ui.verification

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.R
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.ui.core.elements.OTPElement
import com.stripe.android.ui.core.elements.OTPElementUI
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.uicore.DefaultStripeTheme
import kotlinx.coroutines.delay

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
                isSendingNewCode = false,
                errorMessage = null,
                focusRequester = remember { FocusRequester() },
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

@OptIn(ExperimentalComposeUiApi::class)
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

    val viewState by viewModel.viewState.collectAsState()

    onVerificationCompleted?.let {
        viewModel.onVerificationCompleted = it
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester: FocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(viewState.isProcessing) {
        if (viewState.isProcessing) {
            focusManager.clearFocus(true)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(viewState.requestFocus) {
        if (viewState.requestFocus) {
            // Workaround for keyboard not being shown when focus is requested in a Dialog
            // https://issuetracker.google.com/issues/204502668
            delay(200)
            focusRequester.requestFocus()
            keyboardController?.show()
            viewModel.onFocusRequested()
        }
    }

    LaunchedEffect(viewState.didSendNewCode) {
        if (viewState.didSendNewCode) {
            Toast.makeText(context, R.string.verification_code_sent, Toast.LENGTH_SHORT).show()
            viewModel.didShowCodeSentNotification()
        }
    }

    VerificationBody(
        headerStringResId = headerStringResId,
        messageStringResId = messageStringResId,
        showChangeEmailMessage = showChangeEmailMessage,
        redactedPhoneNumber = viewModel.linkAccount.redactedPhoneNumber,
        email = viewModel.linkAccount.email,
        otpElement = viewModel.otpElement,
        isProcessing = viewState.isProcessing,
        isSendingNewCode = viewState.isSendingNewCode,
        errorMessage = viewState.errorMessage,
        focusRequester = focusRequester,
        onBack = viewModel::onBack,
        onChangeEmailClick = viewModel::onChangeEmailClicked,
        onResendCodeClick = viewModel::resendCode,
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
    isSendingNewCode: Boolean,
    errorMessage: ErrorMessage?,
    focusRequester: FocusRequester,
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
        DefaultStripeTheme {
            OTPElementUI(
                enabled = !isProcessing,
                element = otpElement,
                modifier = Modifier.padding(vertical = 10.dp),
                colors = MaterialTheme.linkColors.otpElementColors,
                focusRequester = focusRequester
            )
        }

        if (showChangeEmailMessage) {
            ChangeEmailRow(
                email = email,
                isProcessing = isProcessing,
                onChangeEmailClick = onChangeEmailClick,
            )
        }

        AnimatedVisibility(visible = errorMessage != null) {
            ErrorText(
                text = errorMessage?.getMessage(LocalContext.current.resources).orEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        ResendCodeButton(
            isProcessing = isProcessing,
            isSendingNewCode = isSendingNewCode,
            onClick = onResendCodeClick,
        )
    }
}

@Composable
private fun ChangeEmailRow(
    email: String,
    isProcessing: Boolean,
    onChangeEmailClick: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.verification_not_email, email),
            modifier = Modifier.weight(weight = 1f, fill = false),
            color = MaterialTheme.colors.onSecondary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.body2
        )
        Text(
            text = stringResource(id = R.string.verification_change_email),
            modifier = Modifier
                .padding(start = 4.dp)
                .clickable(
                    enabled = !isProcessing,
                    onClick = onChangeEmailClick
                ),
            color = MaterialTheme.linkColors.actionLabel,
            maxLines = 1,
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
private fun ResendCodeButton(
    isProcessing: Boolean,
    isSendingNewCode: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.linkColors.componentBorder,
                shape = MaterialTheme.linkShapes.extraSmall,
            )
            .clip(shape = MaterialTheme.linkShapes.extraSmall)
            .clickable(
                enabled = !isProcessing && !isSendingNewCode,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center
    ) {
        val textAlpha = if (isProcessing) {
            ContentAlpha.disabled
        } else if (isSendingNewCode) {
            0f
        } else {
            ContentAlpha.high
        }

        Text(
            text = stringResource(id = R.string.verification_resend),
            style = MaterialTheme.typography.button,
            color = MaterialTheme.colors.onPrimary,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .alpha(textAlpha),
        )

        CircularProgressIndicator(
            color = MaterialTheme.colors.onPrimary,
            strokeWidth = 2.dp,
            modifier = Modifier
                .size(18.dp)
                .alpha(if (isSendingNewCode) 1f else 0f),
        )
    }
}
