package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.LinkAuthFlow
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.link.ui.verification.withAuthFlowState
import com.stripe.android.model.LinkBrand
import com.stripe.android.uicore.elements.OTPElementFactory
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The Link verification dialog opened for an inline verification attempt. It shows the same [authFlow] as the card,
 * so the code that was sent, its errors and resend cooldown carry over. Closing it doesn't end the attempt.
 */
internal class EmbeddedLinkVerificationDialogSession(
    private val authFlow: LinkAuthFlow,
    linkAccount: LinkAccount,
    linkBrand: LinkBrand,
    private val scope: CoroutineScope,
    private val onDismiss: () -> Unit,
) {
    val otpElement = OTPElementFactory.create()

    val phoneNumberController = PhoneNumberController.createPhoneNumberController(
        initiallySelectedCountryCode = linkAccount.phoneNumberCountry,
    )

    private val requestFocus = MutableStateFlow(true)

    private val baseViewState = VerificationViewState(
        isProcessing = false,
        requestFocus = true,
        errorMessage = null,
        isSendingNewCode = false,
        didSendNewCode = false,
        redactedPhoneNumber = linkAccount.redactedPhoneNumber,
        email = linkAccount.email,
        isDialog = true,
        // The card's "Switch accounts" is the way to use another account.
        allowLogout = false,
        defaultPayment = null,
        consentSection = null,
        isProcessingWebAuth = false,
        linkBrand = linkBrand,
        authFlow = null,
    )

    val viewState: StateFlow<VerificationViewState> = combineAsStateFlow(
        authFlow.state,
        requestFocus,
    ) { authFlowState, requestFocus ->
        baseViewState.copy(requestFocus = requestFocus).withAuthFlowState(authFlowState, isNewInput = false)
    }

    init {
        scope.launch {
            // Collects every completed code, so the same code entered again after a failure is resubmitted.
            otpElement.otpCompleteFlow.collect { code -> authFlow.confirm(code) }
        }
        scope.launch {
            otpElement.controller.fieldValue.drop(1).filter { it.isNotEmpty() }.collect {
                authFlow.clearError()
            }
        }
        scope.launch {
            authFlow.state.map { it.inputRevision }.distinctUntilChanged().drop(1).collect {
                otpElement.controller.reset()
                requestFocus.value = true
            }
        }
    }

    fun onBack() {
        onDismiss()
    }

    fun onNavigateBack() {
        authFlow.goBack()
    }

    fun onResendCode() {
        authFlow.resend()
    }

    fun onEmailCode() {
        authFlow.sendToEmail()
    }

    fun onPhoneNumberSubmitted() {
        authFlow.submitPhoneNumber(
            phoneNumberController.getE164PhoneNumber(phoneNumberController.fieldValue.value)
        )
    }

    fun onChangeEmail() {
        authFlow.cancel(switchAccount = true)
    }

    fun onFocusRequested() {
        requestFocus.value = false
    }

    fun close() {
        scope.cancel()
    }
}
