@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.networkinglinkverification

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement

internal class NetworkingLinkVerificationPreviewParameterProvider :
    PreviewParameterProvider<NetworkingLinkVerificationState> {
    override val values = sequenceOf(
        loading(),
        canonical(),
        submitting(),
        otpError(),
        unknownError()
    )

    private fun canonical() = NetworkingLinkVerificationState(
        payload = payload(),
        confirmVerification = Uninitialized
    )

    private fun submitting() = NetworkingLinkVerificationState(
        payload = payload(),
        confirmVerification = Loading()
    )

    private fun otpError() = NetworkingLinkVerificationState(
        payload = payload(),
        confirmVerification = Fail(
            ConfirmVerification.OTPError(
                "12345678",
                ConfirmVerification.OTPError.Type.EMAIL_CODE_EXPIRED
            )
        )
    )

    private fun unknownError() = NetworkingLinkVerificationState(
        payload = payload(),
        confirmVerification = Fail(
            Exception("Random error")
        )
    )

    private fun payload() = Success(
        NetworkingLinkVerificationState.Payload(
            email = "theLargestEmailYoulleverseeThatCouldBreakALayout@email.com",
            phoneNumber = "12345678",
            otpElement = OTPElement(
                IdentifierSpec.Generic("otp"),
                OTPController()
            ),
            consumerSessionClientSecret = "12345678"
        )
    )

    private fun loading() = NetworkingLinkVerificationState(
        payload = Loading(),
    )
}
