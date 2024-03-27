package com.stripe.android.financialconnections.features.linkstepupverification

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement

internal class LinkStepUpVerificationPreviewParameterProvider :
    PreviewParameterProvider<LinkStepUpVerificationState> {
    override val values = sequenceOf(
        loading(),
        canonical(),
        submitting(),
        otpError(),
        randomError()
    )

    private fun canonical() = LinkStepUpVerificationState(
        payload = payload(),
        confirmVerification = Uninitialized
    )

    private fun submitting() = LinkStepUpVerificationState(
        payload = payload(),
        confirmVerification = Loading()
    )

    private fun otpError() = LinkStepUpVerificationState(
        payload = payload(),
        confirmVerification = Fail(
            ConfirmVerification.OTPError(
                "12345678",
                ConfirmVerification.OTPError.Type.EMAIL_CODE_EXPIRED
            )
        )
    )

    private fun randomError() = LinkStepUpVerificationState(
        payload = payload(),
        confirmVerification = Fail(
            Exception("Random error")
        )
    )

    private fun payload() = Success(
        LinkStepUpVerificationState.Payload(
            email = "theLargestEmailYoulleverseeThatCouldBreakALayout@email.com",
            phoneNumber = "12345678",
            otpElement = OTPElement(
                IdentifierSpec.Generic("otp"),
                OTPController()
            ),
            consumerSessionClientSecret = "12345678"
        )
    )

    private fun loading() = LinkStepUpVerificationState(
        payload = Loading(),
    )
}
