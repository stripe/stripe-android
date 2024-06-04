package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement

internal class NetworkingSaveToLinkVerificationPreviewParameterProvider :
    PreviewParameterProvider<NetworkingSaveToLinkVerificationState> {
    override val values = sequenceOf(
        loading(),
        canonical(),
        submitting(),
        otpError(),
        randomError(),
        returningUser()
    )

    private fun canonical() = NetworkingSaveToLinkVerificationState(
        payload = Success(payload()),
        confirmVerification = Uninitialized
    )

    private fun submitting() = NetworkingSaveToLinkVerificationState(
        payload = Success(payload()),
        confirmVerification = Loading()
    )

    private fun otpError() = NetworkingSaveToLinkVerificationState(
        payload = Success(payload()),
        confirmVerification = Fail(
            ConfirmVerification.OTPError(
                "12345678",
                ConfirmVerification.OTPError.Type.EMAIL_CODE_EXPIRED
            )
        )
    )

    private fun randomError() = NetworkingSaveToLinkVerificationState(
        payload = Success(payload()),
        confirmVerification = Fail(
            Exception("Random error")
        )
    )

    private fun returningUser() = NetworkingSaveToLinkVerificationState(
        payload = Success(
            payload().copy(showNotNowButton = true)
        ),
        confirmVerification = Success(Unit)
    )

    private fun payload() = NetworkingSaveToLinkVerificationState.Payload(
        email = "theLargestEmailYoulleverseeThatCouldBreakALayout@email.com",
        phoneNumber = "12345678",
        otpElement = OTPElement(
            IdentifierSpec.Generic("otp"),
            OTPController()
        ),
        showNotNowButton = false,
        consumerSessionClientSecret = "12345678"
    )

    private fun loading() = NetworkingSaveToLinkVerificationState(
        payload = Loading(),
    )
}
