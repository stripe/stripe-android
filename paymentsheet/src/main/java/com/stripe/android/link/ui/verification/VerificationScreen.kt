package com.stripe.android.link.ui.verification

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.utils.LINK_DEFAULT_ANIMATION_DELAY_MILLIS
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.CircularProgressIndicator
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementColors
import com.stripe.android.uicore.elements.OTPElementUI
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay

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
            delay(LINK_DEFAULT_ANIMATION_DELAY_MILLIS)
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
        onBack = {
            focusManager.clearFocus(true)
            viewModel.onBack()
        },
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
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textSecondary,
        )

        OTPView(
            state = state,
            otpElement = otpElement,
            focusRequester = focusRequester
        )

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

        if (state.isDialog) {
            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                text = state.email,
                style = LinkTheme.typography.detail,
                color = LinkTheme.colors.textTertiary,
            )
        }
    }
}

@Composable
private fun OTPView(
    state: VerificationViewState,
    otpElement: OTPElement,
    focusRequester: FocusRequester
) {
    StripeThemeForLink {
        OTPElementUI(
            enabled = !state.isProcessing,
            element = otpElement,
            otpInputPlaceholder = " ",
            middleSpacing = 8.dp,
            modifier = Modifier
                .testTag(VERIFICATION_OTP_TAG)
                .padding(vertical = 10.dp),
            colors = OTPElementColors(
                unselectedBorder = LinkTheme.colors.surfaceSecondary,
                selectedBorder = LinkTheme.colors.borderSelected,
                placeholder = LinkTheme.colors.textPrimary,
                background = LinkTheme.colors.surfaceSecondary
            ),
            focusRequester = focusRequester
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
                    .align(Alignment.Center)
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
                    contentDescription = stringResource(com.stripe.android.R.string.stripe_cancel),
                    tint = LinkTheme.colors.iconSecondary,
                )
            }
        }

        Text(
            text = stringResource(R.string.stripe_verification_dialog_header),
            modifier = Modifier
                .testTag(VERIFICATION_TITLE_TAG)
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
        )
    } else {
        Text(
            text = stringResource(R.string.stripe_verification_dialog_header),
            modifier = Modifier
                .testTag(VERIFICATION_TITLE_TAG)
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
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
            color = LinkTheme.colors.textTertiary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = LinkTheme.typography.detail,
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
            color = LinkTheme.colors.textBrand,
            maxLines = 1,
            style = LinkTheme.typography.detail,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ResendCodeButton(
    isProcessing: Boolean,
    isSendingNewCode: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag(VERIFICATION_RESEND_CODE_BUTTON_TAG)
            .padding(top = 12.dp)
            .clip(shape = LinkTheme.shapes.extraSmall)
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
            style = LinkTheme.typography.detailEmphasized,
            color = LinkTheme.colors.textBrand,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .alpha(textAlpha),
        )

        AnimatedVisibility(
            visible = isSendingNewCode
        ) {
            CircularProgressIndicator(
                color = LinkTheme.colors.textPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .testTag(VERIFICATION_RESEND_LOADER_TAG)
                    .size(18.dp)
            )
        }
    }
}

@Preview
@Composable
private fun VerificationBodyPreview() {
    DefaultLinkTheme {
        VerificationBody(
            state = VerificationViewState(
                isDialog = true,
                redactedPhoneNumber = "123-456-7890",
                email = "",
                isProcessing = false,
                errorMessage = null,
                isSendingNewCode = true,
                didSendNewCode = false,
                requestFocus = false,
            ),
            otpElement = OTPSpec.transform(),
            onBack = {},
            onChangeEmailClick = {},
            onResendCodeClick = {}
        )
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
