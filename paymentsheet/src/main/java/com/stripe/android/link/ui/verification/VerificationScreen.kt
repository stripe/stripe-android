package com.stripe.android.link.ui.verification

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementUI
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun VerificationScreen(
    viewModel: VerificationViewModel
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester: FocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.isProcessing) {
        if (state.isProcessing) {
            focusManager.clearFocus(true)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(state.requestFocus) {
        if (state.requestFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
            viewModel.onFocusRequested()
        }
    }

    LaunchedEffect(state.didSendNewCode) {
        if (state.didSendNewCode) {
            Toast.makeText(context, R.string.stripe_verification_code_sent, Toast.LENGTH_SHORT).show()
            viewModel.didShowCodeSentNotification()
        }
    }

    VerificationBody(
        state = state,
        otpElement = viewModel.otpElement,
        focusRequester = focusRequester,
        onBack = viewModel::onBack,
        onChangeEmailClick = viewModel::onChangeEmailButtonClicked,
        onResendCodeClick = viewModel::resendCode
    )
}

@Composable
internal fun VerificationBody(
    state: VerificationViewState,
    otpElement: OTPElement,
    onBack: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onResendCodeClick: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    BackHandler(onBack = onBack)

    ScrollableTopLevelColumn {
        Header(
            isDialog = state.isDialog,
            onBackClicked = onBack
        )

        Text(
            text = stringResource(R.string.stripe_link_verification_message, state.redactedPhoneNumber),
            modifier = Modifier
                .testTag(VERIFICATION_SUBTITLE_TAG)
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSecondary
        )

        StripeThemeForLink {
            OTPElementUI(
                enabled = !state.isProcessing,
                element = otpElement,
                modifier = Modifier
                    .testTag(VERIFICATION_OTP_TAG)
                    .padding(vertical = 10.dp),
                colors = MaterialTheme.linkColors.otpElementColors,
                focusRequester = focusRequester
            )
        }

        if (state.isDialog.not()) {
            ChangeEmailRow(
                email = state.email,
                isProcessing = state.isProcessing,
                onChangeEmailClick = onChangeEmailClick,
            )
        }

        AnimatedVisibility(visible = state.errorMessage != null) {
            ErrorText(
                text = state.errorMessage?.resolve(LocalContext.current).orEmpty(),
                modifier = Modifier
                    .testTag(VERIFICATION_ERROR_TAG)
                    .fillMaxWidth()
            )
        }

        ResendCodeButton(
            isProcessing = state.isProcessing,
            isSendingNewCode = state.isSendingNewCode,
            onClick = onResendCodeClick,
        )
    }
}

@Composable
private fun Header(
    isDialog: Boolean,
    onBackClicked: () -> Unit
) {
    if (isDialog) {
        Box(
            modifier = Modifier
                .padding(
                    bottom = 8.dp
                )
                .fillMaxWidth()
        ) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag(VERIFICATION_HEADER_IMAGE_TAG),
                painter = painterResource(R.drawable.stripe_link_logo),
                contentDescription = stringResource(com.stripe.android.R.string.stripe_link),
            )

            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .testTag(VERIFICATION_HEADER_BUTTON_TAG),
                onClick = onBackClicked
            ) {
                Icon(
                    painter = painterResource(R.drawable.stripe_link_close),
                    contentDescription = stringResource(com.stripe.android.R.string.stripe_cancel)
                )
            }
        }

        Text(
            text = stringResource(R.string.stripe_verification_dialog_header),
            modifier = Modifier
                .testTag(VERIFICATION_TITLE_TAG)
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary
        )
    } else {
        Text(
            text = stringResource(R.string.stripe_verification_dialog_header),
            modifier = Modifier
                .testTag(VERIFICATION_TITLE_TAG)
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary
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
            text = stringResource(id = R.string.stripe_verification_not_email, email),
            modifier = Modifier.weight(weight = 1f, fill = false),
            color = MaterialTheme.colors.onSecondary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.body2
        )
        Text(
            text = stringResource(id = R.string.stripe_verification_change_email),
            modifier = Modifier
                .testTag(VERIFICATION_CHANGE_EMAIL_TAG)
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
            .testTag(VERIFICATION_RESEND_CODE_BUTTON_TAG)
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
            text = stringResource(id = R.string.stripe_verification_resend),
            style = MaterialTheme.typography.button,
            color = MaterialTheme.colors.onPrimary,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .alpha(textAlpha),
        )

        AnimatedVisibility(
            visible = isSendingNewCode
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colors.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .testTag(VERIFICATION_RESEND_LOADER_TAG)
                    .size(18.dp)
            )
        }
    }
}

internal const val VERIFICATION_TITLE_TAG = "verification_title"
internal const val VERIFICATION_SUBTITLE_TAG = "verification_subtitle"
internal const val VERIFICATION_OTP_TAG = "verification_otp_tag"
internal const val VERIFICATION_CHANGE_EMAIL_TAG = "verification_change_email_tag"
internal const val VERIFICATION_ERROR_TAG = "verification_error_tag"
internal const val VERIFICATION_RESEND_LOADER_TAG = "verification_resend_loader_tag"
internal const val VERIFICATION_RESEND_CODE_BUTTON_TAG = "verification_resend_code_button_tag"
internal const val VERIFICATION_HEADER_IMAGE_TAG = "verification_header_image_tag"
internal const val VERIFICATION_HEADER_BUTTON_TAG = "verification_header_button_tag"
