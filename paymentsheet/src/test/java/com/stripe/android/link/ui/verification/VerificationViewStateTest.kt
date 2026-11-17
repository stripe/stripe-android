package com.stripe.android.link.ui.verification

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.VerificationType
import org.junit.Test

internal class VerificationViewStateTest {

    @Test
    fun `auth flow state is applied to the view state`() {
        val viewState = viewState().withAuthFlowState(
            state = authFlowState(
                isLoading = true,
                errorMessage = resolvableString("Wrong code"),
                canGoBack = true,
                canSubmitCode = false,
                canResend = false,
                resendSecondsRemaining = 7,
                canSendToEmail = true,
            ),
            isNewInput = false,
        )

        assertThat(viewState.isProcessing).isTrue()
        assertThat(viewState.isSendingNewCode).isFalse()
        assertThat(viewState.errorMessage).isEqualTo(resolvableString("Wrong code"))
        assertThat(viewState.requestFocus).isFalse()
        assertThat(viewState.authFlow).isEqualTo(
            VerificationViewState.AuthFlowViewState(
                screen = LinkAuthFlowState.Screen.Otp,
                recipient = RECIPIENT,
                canGoBack = true,
                codeEntryEnabled = false,
                canSendToEmail = true,
                canResend = false,
                resendSecondsRemaining = 7,
                phoneNumberLastTwoDigits = "42",
            )
        )
    }

    @Test
    fun `resending shows a new code is being sent instead of processing`() {
        val viewState = viewState().withAuthFlowState(
            state = authFlowState(isLoading = true, isResending = true),
            isNewInput = false,
        )

        assertThat(viewState.isProcessing).isFalse()
        assertThat(viewState.isSendingNewCode).isTrue()
    }

    @Test
    fun `new input on the code screen requests focus`() {
        val viewState = viewState().withAuthFlowState(
            state = authFlowState(screen = LinkAuthFlowState.Screen.Otp),
            isNewInput = true,
        )

        assertThat(viewState.requestFocus).isTrue()
    }

    @Test
    fun `new input on another screen does not request focus`() {
        val viewState = viewState().withAuthFlowState(
            state = authFlowState(screen = LinkAuthFlowState.Screen.PhoneMatch),
            isNewInput = true,
        )

        assertThat(viewState.requestFocus).isFalse()
    }

    @Test
    fun `pending focus request is kept`() {
        val viewState = viewState(requestFocus = true).withAuthFlowState(
            state = authFlowState(),
            isNewInput = false,
        )

        assertThat(viewState.requestFocus).isTrue()
    }

    private fun viewState(requestFocus: Boolean = false) = VerificationViewState(
        isProcessing = false,
        requestFocus = requestFocus,
        errorMessage = null,
        isSendingNewCode = false,
        didSendNewCode = false,
        redactedPhoneNumber = RECIPIENT,
        email = "email@stripe.com",
        isDialog = true,
        allowLogout = false,
        defaultPayment = null,
        linkBrand = LinkBrand.Link,
        authFlow = null,
    )

    @Suppress("LongParameterList")
    private fun authFlowState(
        screen: LinkAuthFlowState.Screen = LinkAuthFlowState.Screen.Otp,
        isLoading: Boolean = false,
        isResending: Boolean = false,
        errorMessage: com.stripe.android.core.strings.ResolvableString? = null,
        canGoBack: Boolean = false,
        canSubmitCode: Boolean = true,
        canResend: Boolean = true,
        resendSecondsRemaining: Int = 0,
        canSendToEmail: Boolean = false,
    ) = LinkAuthFlowState(
        screen = screen,
        verificationType = VerificationType.SMS,
        step = 1,
        isLoading = isLoading,
        isResending = isResending,
        errorMessage = errorMessage,
        inputRevision = 1,
        invalidCodeRevision = 0,
        isCodeSent = true,
        canGoBack = canGoBack,
        canSubmitCode = canSubmitCode,
        canResend = canResend,
        resendSecondsRemaining = resendSecondsRemaining,
        canSendToEmail = canSendToEmail,
        recipient = RECIPIENT,
        phoneNumberLastTwoDigits = "42",
    )

    private companion object {
        const val RECIPIENT = "+1 (•••) •••-••42"
    }
}
