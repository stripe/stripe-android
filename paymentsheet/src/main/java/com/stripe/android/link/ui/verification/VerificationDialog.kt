package com.stripe.android.link.ui.verification

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.linkViewModel
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun VerificationDialog(
    modifier: Modifier,
    linkAccount: LinkAccount,
    onVerificationSucceeded: () -> Unit,
    changeEmail: () -> Unit,
    onDismissClicked: () -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit
) {
    val viewModel = linkViewModel<VerificationViewModel> { parentComponent ->
        VerificationViewModel.factory(
            parentComponent = parentComponent,
            linkAccount = linkAccount,
            isDialog = true,
            onVerificationSucceeded = onVerificationSucceeded,
            onChangeEmailClicked = changeEmail,
            onDismissClicked = onDismissClicked,
            dismissWithResult = dismissWithResult
        )
    }

    val state by viewModel.viewState.collectAsState()

    VerificationDialogBody(
        modifier = modifier,
        state = state,
        otpElement = viewModel.otpElement,
        onBack = viewModel::onBack,
        onChangeEmailClick = viewModel::onChangeEmailButtonClicked,
        onResendCodeClick = viewModel::resendCode,
        onFocusRequested = viewModel::onFocusRequested,
        didShowCodeSentNotification = viewModel::didShowCodeSentNotification,
        onConsentShown = viewModel::onConsentShown
    )
}

@Composable
internal fun VerificationDialogBody(
    modifier: Modifier = Modifier,
    state: VerificationViewState,
    otpElement: OTPElement,
    onBack: () -> Unit,
    onFocusRequested: () -> Unit,
    didShowCodeSentNotification: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onResendCodeClick: () -> Unit,
    onConsentShown: () -> Unit,
) {
    Box(
        modifier = modifier
    ) {
        Dialog(
            onDismissRequest = onBack,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            // This is a workaround to set a lighter dim in dark mode
            // because the default dim amount is too dark for the dialog
            val dim = if (isSystemInDarkTheme()) DIM_DARK_THEME else DIM_LIGHT_THEME
            (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(dim)

            DefaultLinkTheme {
                Surface(
                    modifier = Modifier.width(360.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = LinkTheme.colors.surfacePrimary
                ) {
                    VerificationBody(
                        state = state,
                        otpElement = otpElement,
                        onBack = onBack,
                        onChangeEmailClick = onChangeEmailClick,
                        onResendCodeClick = onResendCodeClick,
                        onFocusRequested = onFocusRequested,
                        didShowCodeSentNotification = didShowCodeSentNotification,
                        onConsentShown = onConsentShown,
                    )
                }
            }
        }
    }
}

private const val DIM_LIGHT_THEME = 0.8f
private const val DIM_DARK_THEME = 0.3f

@Preview()
@Composable
fun VerificationDialogPreview() {
    DefaultLinkTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LinkTheme.colors.surfacePrimary
        ) {
            VerificationDialogBody(
                state = VerificationViewState(
                    isProcessing = false,
                    isSendingNewCode = false,
                    errorMessage = resolvableString("Test error message"),
                    didSendNewCode = false,
                    requestFocus = false,
                    redactedPhoneNumber = "(...)",
                    email = "email@email.com",
                    defaultPayment = null,
                    isDialog = true,
                    allowLogout = true,
                ),
                otpElement = OTPSpec.transform(),
                onBack = {},
                onChangeEmailClick = {},
                onResendCodeClick = {},
                onFocusRequested = {},
                didShowCodeSentNotification = {},
                onConsentShown = {}
            )
        }
    }
}
