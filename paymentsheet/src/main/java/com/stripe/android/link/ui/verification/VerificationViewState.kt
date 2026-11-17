package com.stripe.android.link.ui.verification

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.wallet.DefaultPaymentUI
import com.stripe.android.model.ConsentUi
import com.stripe.android.model.LinkBrand
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
internal data class VerificationViewState(
    val isProcessing: Boolean,
    val requestFocus: Boolean,
    val errorMessage: ResolvableString?,
    val isSendingNewCode: Boolean,
    val didSendNewCode: Boolean,
    val redactedPhoneNumber: String,
    val email: String,
    val isDialog: Boolean,
    val allowLogout: Boolean,
    val defaultPayment: DefaultPaymentUI?,
    val consentSection: ConsentUi.ConsentSection? = null,
    val isProcessingWebAuth: Boolean = false,
    val linkBrand: LinkBrand,
    /**
     * The state of the multi-factor auth flow, or null when only SMS OTP is supported.
     */
    val authFlow: AuthFlowViewState?,
) : Parcelable {

    @Immutable
    @Parcelize
    data class AuthFlowViewState(
        val screen: LinkAuthFlowState.Screen,
        val recipient: String,
        val canGoBack: Boolean,
        val codeEntryEnabled: Boolean,
        val canSendToEmail: Boolean,
        val canResend: Boolean,
        val resendSecondsRemaining: Int,
        val phoneNumberLastTwoDigits: String?,
    ) : Parcelable
}

/**
 * Applies the state of a [LinkAuthFlow] to this view state.
 *
 * @param isNewInput Whether the flow asks for a new code, which requests focus on the code field.
 */
internal fun VerificationViewState.withAuthFlowState(
    state: LinkAuthFlowState,
    isNewInput: Boolean,
): VerificationViewState {
    return copy(
        isProcessing = state.isLoading && !state.isResending,
        isSendingNewCode = state.isResending,
        errorMessage = state.errorMessage,
        requestFocus = requestFocus || (isNewInput && state.screen == LinkAuthFlowState.Screen.Otp),
        authFlow = VerificationViewState.AuthFlowViewState(
            screen = state.screen,
            recipient = state.recipient,
            canGoBack = state.canGoBack,
            codeEntryEnabled = state.canSubmitCode,
            canSendToEmail = state.canSendToEmail,
            canResend = state.canResend,
            resendSecondsRemaining = state.resendSecondsRemaining,
            phoneNumberLastTwoDigits = state.phoneNumberLastTwoDigits,
        ),
    )
}
