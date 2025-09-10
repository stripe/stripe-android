package com.stripe.android.link.ui.verification

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.AppBarIcon
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkSpinner
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.utils.LINK_DEFAULT_ANIMATION_DELAY_MILLIS
import com.stripe.android.model.ConsentUi
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.SectionStyle
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementColors
import com.stripe.android.uicore.elements.OTPElementUI
import com.stripe.android.uicore.text.Html
import kotlinx.coroutines.delay

/**
 * Common verification body content used in [VerificationScreen] and [VerificationDialog].
 */
@Composable
@Suppress("LongMethod")
internal fun VerificationBody(
    state: VerificationViewState,
    otpElement: OTPElement,
    onBack: () -> Unit,
    onFocusRequested: () -> Unit,
    didShowCodeSentNotification: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onResendCodeClick: () -> Unit,
    onConsentShown: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester: FocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffects(
        state = state,
        focusManager = focusManager,
        keyboardController = keyboardController,
        focusRequester = focusRequester,
        onFocusRequested = onFocusRequested,
        context = context,
        didShowCodeSentNotification = didShowCodeSentNotification
    )

    VerificationBodyContainer(
        isDialog = state.isDialog,
        onBackClicked = {
            focusManager.clearFocus()
            onBack()
        }
    ) {
        Title(
            isDialog = state.isDialog
        )

        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = stringResource(R.string.stripe_link_verification_message_short, state.redactedPhoneNumber),
            modifier = Modifier
                .testTag(VERIFICATION_SUBTITLE_TAG)
                .fillMaxWidth(),
            textAlign = TextAlign.Companion.Center,
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textTertiary
        )

        Spacer(modifier = Modifier.size(24.dp))

        StripeThemeForLink(sectionStyle = SectionStyle.Bordered) {
            OTPElementUI(
                enabled = !state.isProcessing,
                element = otpElement,
                middleSpacing = 8.dp,
                boxSpacing = 8.dp,
                otpInputPlaceholder = " ",
                boxShape = LinkTheme.shapes.default,
                modifier = Modifier
                    // 48dp per OTP box plus 8dp per space
                    .width(328.dp)
                    .testTag(VERIFICATION_OTP_TAG),
                colors = OTPElementColors(
                    selectedBorder = LinkTheme.colors.borderSelected,
                    placeholder = LinkTheme.colors.textPrimary,
                    selectedBackground = LinkTheme.colors.surfacePrimary,
                    background = LinkTheme.colors.surfaceSecondary,
                    unselectedBorder = LinkTheme.colors.surfaceSecondary
                ),
                focusRequester = focusRequester,
                selectedStrokeWidth = 1.5.dp,
            )
        }

        AnimatedVisibility(visible = state.errorMessage != null) {
            ErrorText(
                text = state.errorMessage?.resolve(LocalContext.current).orEmpty(),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag(VERIFICATION_ERROR_TAG)
                    .fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.size(24.dp))
        ResendCodeButton(
            isProcessing = state.isProcessing,
            isSendingNewCode = state.isSendingNewCode,
            onClick = onResendCodeClick,
        )

        state.consentSection?.let { consentSection ->
            ConsentSection(consentSection)
            LaunchedEffect(consentSection) {
                onConsentShown()
            }
        }

        if (state.allowLogout) {
            Spacer(modifier = Modifier.size(24.dp))
            ChangeEmailRow(
                email = state.email,
                isProcessing = state.isProcessing,
                onChangeEmailClick = onChangeEmailClick,
            )
        }

        Spacer(modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun LaunchedEffects(
    state: VerificationViewState,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    onFocusRequested: () -> Unit,
    context: Context,
    didShowCodeSentNotification: () -> Unit
) {
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
            onFocusRequested()
        }
    }

    LaunchedEffect(state.didSendNewCode) {
        if (state.didSendNewCode) {
            Toast.makeText(context, R.string.stripe_verification_code_sent, Toast.LENGTH_SHORT).show()
            didShowCodeSentNotification()
        }
    }
}

/**
 * A wrapper for the content of the verification screen.
 *
 * @param isDialog whether the screen is displayed as a dialog or not
 * @param content the content to be displayed
 */
@Composable
private fun VerificationBodyContainer(
    isDialog: Boolean,
    onBackClicked: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (isDialog) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    modifier = Modifier
                        .testTag(VERIFICATION_HEADER_IMAGE_TAG),
                    painter = painterResource(R.drawable.stripe_link_logo),
                    contentDescription = stringResource(com.stripe.android.R.string.stripe_link),
                )

                Spacer(modifier = Modifier.weight(1f))

                AppBarIcon(
                    icon = R.drawable.stripe_link_close,
                    contentDescription = stringResource(id = com.stripe.android.R.string.stripe_close),
                    onPressed = onBackClicked,
                    modifier = Modifier.testTag(VERIFICATION_HEADER_BUTTON_TAG),
                )
            }

            Column(
                modifier = Modifier.padding(
                    top = 2.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                content = content
            )
        }
    } else {
        ScrollableTopLevelColumn(content = content)
    }
}

@Composable
private fun Title(
    isDialog: Boolean,
) {
    if (isDialog) {
        Text(
            text = stringResource(R.string.stripe_verification_dialog_header),
            modifier = Modifier
                .testTag(VERIFICATION_TITLE_TAG),
            textAlign = TextAlign.Companion.Center,
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary
        )
    } else {
        Text(
            text = stringResource(R.string.stripe_verification_dialog_header),
            modifier = Modifier
                .testTag(VERIFICATION_TITLE_TAG)
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Companion.Center,
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary
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
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = email,
            modifier = Modifier.weight(weight = 1f, fill = false),
            color = LinkTheme.colors.textTertiary,
            overflow = TextOverflow.Companion.Ellipsis,
            maxLines = 1,
            style = LinkTheme.typography.body
        )
        Text(
            text = stringResource(id = R.string.stripe_verification_change_email_new),
            modifier = Modifier
                .testTag(VERIFICATION_CHANGE_EMAIL_TAG)
                .padding(start = 4.dp)
                .clickable(
                    enabled = !isProcessing,
                    onClick = onChangeEmailClick
                ),
            color = LinkTheme.colors.textBrand,
            maxLines = 1,
            style = LinkTheme.typography.bodyEmphasized
        )
    }
}

@Composable
internal fun ResendCodeButton(
    isProcessing: Boolean,
    isSendingNewCode: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag(VERIFICATION_RESEND_CODE_BUTTON_TAG)
            .clickable(
                enabled = !isProcessing && !isSendingNewCode,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Companion.Center
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
            style = LinkTheme.typography.bodyEmphasized,
            color = LinkTheme.colors.textBrand,
            modifier = Modifier
                .alpha(textAlpha),
        )

        AnimatedVisibility(
            visible = isSendingNewCode
        ) {
            LinkSpinner(
                filledColor = LinkTheme.colors.textPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .testTag(VERIFICATION_RESEND_LOADER_TAG)
                    .size(18.dp)
            )
        }
    }
}

@Composable
private fun ConsentSection(
    consentSection: ConsentUi.ConsentSection,
) {
    Html(
        modifier = Modifier.padding(top = 8.dp),
        html = consentSection.disclaimer,
        style = LinkTheme.typography.caption.copy(textAlign = TextAlign.Center),
        color = LinkTheme.colors.textTertiary,
    )
}

@PreviewLightDark
@Composable
private fun Preview() {
    DefaultLinkTheme {
        Surface {
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = LinkTheme.colors.surfacePrimary,
            ) {
                VerificationBody(
                    state = VerificationViewState(
                        isProcessing = false,
                        requestFocus = false,
                        errorMessage = null,
                        isSendingNewCode = false,
                        didSendNewCode = false,
                        redactedPhoneNumber = "(•••) ••• ••55",
                        email = "email@email.com",
                        defaultPayment = null,
                        isDialog = true,
                        allowLogout = false,
                        consentSection = ConsentUi.ConsentSection(
                            disclaimer = "By continuing you’ll share your name, email, and phone with [Merchant]"
                        )
                    ),
                    otpElement = OTPElement(
                        identifier = IdentifierSpec.Generic("otp"),
                        controller = OTPController(),
                    ),
                    onBack = {},
                    onFocusRequested = {},
                    didShowCodeSentNotification = {},
                    onChangeEmailClick = {},
                    onResendCodeClick = {},
                    onConsentShown = {}
                )
            }
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
