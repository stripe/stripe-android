package com.stripe.android.link.ui.verification

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.model.LinkBrand
import com.stripe.android.uicore.elements.OTPElementFactory
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun VerificationScreen(
    viewModel: VerificationViewModel
) {
    val state by viewModel.viewState.collectAsState()

    // Takes precedence over the screen-level back handler in LinkContent while there's a previous step.
    BackHandler(enabled = state.authFlow?.canGoBack == true) {
        viewModel.onNavigateBack()
    }

    VerificationBody(
        state = state,
        otpElement = viewModel.otpElement,
        phoneNumberController = viewModel.phoneNumberController,
        onBack = viewModel::onBack,
        onNavigateBack = { viewModel.onNavigateBack() },
        onChangeEmailClick = viewModel::onChangeEmailButtonClicked,
        onResendCodeClick = viewModel::resendCode,
        onEmailCodeClick = viewModel::onEmailCodeClicked,
        onPhoneNumberSubmitted = viewModel::onPhoneNumberSubmitted,
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
                    authFlow = null,
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
                    linkBrand = LinkBrand.Link,
                ),
                otpElement = OTPElementFactory.create(),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                onBack = {},
                onNavigateBack = {},
                onChangeEmailClick = {},
                onResendCodeClick = {},
                onEmailCodeClick = {},
                onPhoneNumberSubmitted = {},
                onFocusRequested = {},
                didShowCodeSentNotification = {},
                onConsentShown = {},
            )
        }
    }
}
