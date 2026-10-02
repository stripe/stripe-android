package com.stripe.android.link.ui.verification

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.AppBarIcon
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkLoadingScreen
import com.stripe.android.link.ui.LinkLogoStyle
import com.stripe.android.link.ui.LinkSpinner
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.logoRes
import com.stripe.android.link.utils.LINK_DEFAULT_ANIMATION_DELAY_MILLIS
import com.stripe.android.model.ConsentUi
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.SectionStyle
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementColors
import com.stripe.android.uicore.elements.OTPElementUI
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.text.Html
import kotlinx.coroutines.delay
import com.stripe.android.ui.core.R as StripeUiCoreR

/**
 * Common verification body content used in [VerificationScreen] and [VerificationDialog].
 */
@Composable
@Suppress("LongMethod")
internal fun VerificationBody(
    state: VerificationViewState,
    otpElement: OTPElement,
    phoneNumberController: PhoneNumberController,
    onBack: () -> Unit,
    onNavigateBack: () -> Unit,
    onFocusRequested: () -> Unit,
    didShowCodeSentNotification: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onResendCodeClick: () -> Unit,
    onEmailCodeClick: () -> Unit,
    onPhoneNumberSubmitted: () -> Unit,
    onConsentShown: () -> Unit,
) {
    if (state.isProcessingWebAuth) {
        VerificationBodyContainer(
            isDialog = state.isDialog,
            linkBrand = state.linkBrand,
            canGoBack = false,
            onBackClicked = onBack,
            onNavigateBackClicked = onNavigateBack,
        ) {
            LinkLoadingScreen()
        }
        return
    }

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
        linkBrand = state.linkBrand,
        canGoBack = state.authFlow?.canGoBack == true,
        onBackClicked = {
            focusManager.clearFocus()
            onBack()
        },
        onNavigateBackClicked = {
            focusManager.clearFocus()
            onNavigateBack()
        },
    ) {
        when (state.authFlow?.screen) {
            LinkAuthFlowState.Screen.PhoneMatch -> {
                PhoneMatchContent(
                    phoneNumberLastTwoDigits = state.authFlow.phoneNumberLastTwoDigits,
                    phoneNumberController = phoneNumberController,
                    isProcessing = state.isProcessing,
                    errorMessage = state.errorMessage,
                    onContinueClick = {
                        focusManager.clearFocus()
                        onPhoneNumberSubmitted()
                    },
                )
            }
            LinkAuthFlowState.Screen.Loading -> {
                StatusContent(isDialog = state.isDialog, message = null)
            }
            LinkAuthFlowState.Screen.Blocked -> {
                StatusContent(isDialog = state.isDialog, message = state.errorMessage)
            }
            LinkAuthFlowState.Screen.Otp, null -> {
                OtpContent(
                    state = state,
                    otpElement = otpElement,
                    focusRequester = focusRequester,
                    onResendCodeClick = onResendCodeClick,
                    onEmailCodeClick = onEmailCodeClick,
                    onConsentShown = onConsentShown,
                )
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
private fun ColumnScope.OtpContent(
    state: VerificationViewState,
    otpElement: OTPElement,
    focusRequester: FocusRequester,
    onResendCodeClick: () -> Unit,
    onEmailCodeClick: () -> Unit,
    onConsentShown: () -> Unit,
) {
    Title(
        isDialog = state.isDialog
    )

    Spacer(modifier = Modifier.size(8.dp))

    Text(
        text = stringResource(
            R.string.stripe_link_verification_message_short,
            state.authFlow?.recipient ?: state.redactedPhoneNumber,
        ),
        modifier = Modifier
            .testTag(VERIFICATION_SUBTITLE_TAG)
            .fillMaxWidth(),
        textAlign = TextAlign.Companion.Center,
        style = LinkTheme.typography.body,
        color = LinkTheme.colors.textTertiary
    )

    Spacer(modifier = Modifier.size(24.dp))

    OtpCodeInput(
        enabled = !state.isProcessing && state.authFlow?.codeEntryEnabled != false,
        otpElement = otpElement,
        focusRequester = focusRequester,
    )

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
    if (state.authFlow != null) {
        AuthFlowActions(
            authFlow = state.authFlow,
            isProcessing = state.isProcessing,
            onResendCodeClick = onResendCodeClick,
            onEmailCodeClick = onEmailCodeClick,
        )
    } else {
        ResendCodeButton(
            isProcessing = state.isProcessing,
            isSendingNewCode = state.isSendingNewCode,
            onClick = onResendCodeClick,
        )
    }

    state.consentSection?.let { consentSection ->
        ConsentSection(consentSection)
        LaunchedEffect(consentSection) {
            onConsentShown()
        }
    }
}

@Composable
private fun AuthFlowActions(
    authFlow: VerificationViewState.AuthFlowViewState,
    isProcessing: Boolean,
    onResendCodeClick: () -> Unit,
    onEmailCodeClick: () -> Unit,
) {
    val resendLabel = if (authFlow.resendSecondsRemaining > 0) {
        stringResource(R.string.stripe_link_auth_resend_countdown, authFlow.resendSecondsRemaining)
    } else {
        stringResource(R.string.stripe_verification_resend)
    }

    if (LinkAuthFlowState.Action.Email !in authFlow.actions) {
        if (authFlow.resendSecondsRemaining > 0) {
            Text(
                text = resendLabel,
                modifier = Modifier.testTag(VERIFICATION_RESEND_COUNTDOWN_TAG),
                style = LinkTheme.typography.body,
                color = LinkTheme.colors.textTertiary,
            )
        } else {
            ResendCodeButton(
                isProcessing = isProcessing || (!authFlow.canResend && !authFlow.isResending),
                isSendingNewCode = authFlow.isResending,
                onClick = onResendCodeClick,
            )
        }
        return
    }

    MoreOptionsMenu(
        authFlow = authFlow,
        isProcessing = isProcessing,
        resendLabel = resendLabel,
        onResendCodeClick = onResendCodeClick,
        onEmailCodeClick = onEmailCodeClick,
    )
}

@Composable
private fun OtpCodeInput(
    enabled: Boolean,
    otpElement: OTPElement,
    focusRequester: FocusRequester,
) {
    StripeThemeForLink(sectionStyle = SectionStyle.Bordered) {
        OTPElementUI(
            enabled = enabled,
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
}

@Composable
private fun MoreOptionsMenu(
    authFlow: VerificationViewState.AuthFlowViewState,
    isProcessing: Boolean,
    resendLabel: String,
    onResendCodeClick: () -> Unit,
    onEmailCodeClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        MoreOptionsButton(
            isProcessing = isProcessing,
            isResending = authFlow.isResending,
            onClick = { expanded = true },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LinkTheme.colors.surfacePrimary),
        ) {
            DropdownMenuItem(
                modifier = Modifier.testTag(VERIFICATION_MENU_RESEND_TAG),
                enabled = authFlow.canResend,
                onClick = {
                    expanded = false
                    onResendCodeClick()
                },
            ) {
                Text(
                    text = resendLabel,
                    style = LinkTheme.typography.body,
                    color = LinkTheme.colors.textPrimary,
                    modifier = Modifier.alpha(if (authFlow.canResend) ContentAlpha.high else ContentAlpha.disabled),
                )
            }
            DropdownMenuItem(
                modifier = Modifier.testTag(VERIFICATION_MENU_EMAIL_CODE_TAG),
                enabled = !isProcessing,
                onClick = {
                    expanded = false
                    onEmailCodeClick()
                },
            ) {
                Text(
                    text = stringResource(R.string.stripe_link_auth_email_code),
                    style = LinkTheme.typography.body,
                    color = LinkTheme.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun MoreOptionsButton(
    isProcessing: Boolean,
    isResending: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag(VERIFICATION_MORE_OPTIONS_TAG)
            .clickable(
                enabled = !isProcessing && !isResending,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.stripe_link_auth_more_options),
            style = LinkTheme.typography.bodyEmphasized,
            color = LinkTheme.colors.textBrand,
            modifier = Modifier.alpha(
                when {
                    isResending -> 0f
                    isProcessing -> ContentAlpha.disabled
                    else -> ContentAlpha.high
                }
            ),
        )
        AnimatedVisibility(visible = isResending) {
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
private fun StatusContent(
    isDialog: Boolean,
    message: ResolvableString?,
) {
    Title(isDialog = isDialog)
    Spacer(modifier = Modifier.size(24.dp))
    if (message != null) {
        Text(
            text = message.resolve(LocalContext.current),
            modifier = Modifier
                .testTag(VERIFICATION_STATUS_TAG)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textTertiary,
        )
    } else {
        LinkSpinner(
            filledColor = LinkTheme.colors.textPrimary,
            strokeWidth = 3.dp,
            modifier = Modifier
                .testTag(VERIFICATION_STATUS_TAG)
                .size(24.dp)
        )
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

    val showsOtp = state.authFlow == null || state.authFlow.screen == LinkAuthFlowState.Screen.Otp
    LaunchedEffect(state.requestFocus, showsOtp) {
        if (state.requestFocus && showsOtp) {
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
    linkBrand: LinkBrand,
    canGoBack: Boolean,
    onBackClicked: () -> Unit,
    onNavigateBackClicked: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (isDialog) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canGoBack) {
                    AppBarIcon(
                        icon = R.drawable.stripe_link_back,
                        contentDescription = stringResource(id = StripeUiCoreR.string.stripe_back),
                        onPressed = onNavigateBackClicked,
                        modifier = Modifier.testTag(VERIFICATION_HEADER_BACK_BUTTON_TAG),
                    )
                } else {
                    Image(
                        modifier = Modifier
                            .testTag(VERIFICATION_HEADER_IMAGE_TAG),
                        painter = painterResource(linkBrand.logoRes(LinkLogoStyle.Primary)),
                        contentDescription = linkBrand.brandName(),
                    )
                }

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
                        authFlow = null,
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
                        linkBrand = LinkBrand.Link,
                        consentSection = ConsentUi.ConsentSection(
                            disclaimer = "By continuing you’ll share your name, email, and phone with [Merchant]"
                        )
                    ),
                    otpElement = OTPElement(
                        identifier = FormFieldId.Generic("otp"),
                        controller = OTPController(),
                    ),
                    phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                    onBack = {},
                    onNavigateBack = {},
                    onFocusRequested = {},
                    didShowCodeSentNotification = {},
                    onChangeEmailClick = {},
                    onResendCodeClick = {},
                    onEmailCodeClick = {},
                    onPhoneNumberSubmitted = {},
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
internal const val VERIFICATION_HEADER_BACK_BUTTON_TAG = "verification_header_back_button_tag"
internal const val VERIFICATION_RESEND_COUNTDOWN_TAG = "verification_resend_countdown_tag"
internal const val VERIFICATION_MORE_OPTIONS_TAG = "verification_more_options_tag"
internal const val VERIFICATION_MENU_RESEND_TAG = "verification_menu_resend_tag"
internal const val VERIFICATION_MENU_EMAIL_CODE_TAG = "verification_menu_email_code_tag"
internal const val VERIFICATION_STATUS_TAG = "verification_status_tag"
