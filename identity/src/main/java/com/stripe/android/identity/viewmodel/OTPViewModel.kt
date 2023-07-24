package com.stripe.android.identity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal sealed class OTPViewState {
    object InputtingOTP : OTPViewState() // When user is inputting OTP
    data class SubmittingOTP(
        val otp: String
    ) : OTPViewState() // When OTP is being submitted through Post VerificationPageData

    object ErrorOTP : OTPViewState() // When wrong OTP is submitted
    object RequestingOTP : OTPViewState() // When GenerateOTP is outstanding
    object RequestingCannotVerify : OTPViewState() // When CannotVerify is outstanding
    data class RequestingCannotVerifySuccess(val verificationPageData: VerificationPageData) :
        OTPViewState() // When CannotVerify is successful

    data class RequestingError(val cause: Throwable) :
        OTPViewState() // When there's an error requesting OTP or requesting cannotVerify
}

internal class OTPViewModel(
    private val identityRepository: IdentityRepository,
    private val verificationArgs: IdentityVerificationSheetContract.Args
) : ViewModel() {
    private val _viewState = MutableStateFlow<OTPViewState?>(null)
    val viewState: StateFlow<OTPViewState?> = _viewState

    val otpElement =
        OTPElement(
            identifier = IdentifierSpec.Generic(OTP),
            controller = OTPController()
        )

    internal fun initialize() {
        // transition to submittingOTP when otp is fully input
        viewModelScope.launch {
            otpElement.otpCompleteFlow.collectLatest {
                onValidOTPInput(otp = it)
            }
        }
        generatePhoneOtp()
    }

    private fun onValidOTPInput(otp: String) {
        _viewState.update {
            OTPViewState.SubmittingOTP(otp)
        }
    }

    fun onInputErrorOtp() {
        _viewState.update {
            OTPViewState.ErrorOTP
        }
        otpElement.controller.reset()
    }

    private fun onRequestingError(cause: Throwable) {
        _viewState.update {
            OTPViewState.RequestingError(cause)
        }
    }

    fun generatePhoneOtp() {
        _viewState.update {
            OTPViewState.RequestingOTP
        }
        otpElement.controller.reset()

        viewModelScope.launch {
            runCatching {
                identityRepository.generatePhoneOtp(
                    id = verificationArgs.verificationSessionId,
                    ephemeralKey = verificationArgs.ephemeralKeySecret
                )
            }.fold(
                onSuccess = {
                    onGenerateOtpSuccess()
                },
                onFailure = {
                    onRequestingError(it)
                }
            )
        }
    }

    private fun onGenerateOtpSuccess() {
        _viewState.update {
            OTPViewState.InputtingOTP
        }
    }

    fun onCannotVerifyPhoneOtpClicked() {
        _viewState.update {
            OTPViewState.RequestingCannotVerify
        }
        viewModelScope.launch {
            runCatching {
                identityRepository.cannotVerifyPhoneOtp(
                    id = verificationArgs.verificationSessionId,
                    ephemeralKey = verificationArgs.ephemeralKeySecret
                )
            }.fold(
                onSuccess = {
                    onRequestingCannotVerifySuccess(it)
                },
                onFailure = {
                    onRequestingError(it)
                }
            )
        }
    }

    internal fun resetViewState() {
        _viewState.update { null }
    }

    private fun onRequestingCannotVerifySuccess(verificationPageData: VerificationPageData) {
        _viewState.update {
            OTPViewState.RequestingCannotVerifySuccess(verificationPageData)
        }
    }

    internal class Factory(
        val identityRepository: IdentityRepository,
        val verificationArgs: IdentityVerificationSheetContract.Args
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OTPViewModel(
                identityRepository = identityRepository,
                verificationArgs = verificationArgs
            ) as T
        }
    }

    internal companion object {
        const val OTP = "OTP"
    }
}
