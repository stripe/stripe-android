package com.stripe.android.link.ui.verification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun VerificationScreen(
    viewModel: VerificationViewModel
) {
    val state by viewModel.viewState.collectAsState()

    VerificationBody(
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

@Preview(showBackground = true)
@Composable
fun VerificationPreview() {
    DefaultLinkTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            VerificationBody(
                state = VerificationViewState(
                    isProcessing = false,
                    isSendingNewCode = false,
                    errorMessage = resolvableString("Test error message"),
                    didSendNewCode = false,
                    requestFocus = false,
                    redactedPhoneNumber = "(...)",
                    email = "email@email.com",
                    defaultPayment = null,
                    isDialog = false,
                    allowLogout = true,
                ),
                otpElement = OTPSpec.transform(),
                onBack = {},
                onChangeEmailClick = {},
                onResendCodeClick = {},
                onFocusRequested = {},
                didShowCodeSentNotification = {},
                onConsentShown = {},
            )
        }
    }
}
