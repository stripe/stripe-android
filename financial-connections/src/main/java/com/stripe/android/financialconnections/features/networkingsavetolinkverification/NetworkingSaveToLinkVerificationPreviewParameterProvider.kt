@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.domain.ConfirmVerification
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
        randomError()
    )

    private fun canonical() = NetworkingSaveToLinkVerificationState(
        payload = payload(),
        confirmVerification = Uninitialized
    )

    private fun submitting() = NetworkingSaveToLinkVerificationState(
        payload = payload(),
        confirmVerification = Loading()
    )

    private fun otpError() = NetworkingSaveToLinkVerificationState(
        payload = payload(),
        confirmVerification = Fail(
            ConfirmVerification.OTPError(
                "12345678",
                ConfirmVerification.OTPError.Type.EMAIL_CODE_EXPIRED
            )
        )
    )

    private fun randomError() = NetworkingSaveToLinkVerificationState(
        payload = payload(),
        confirmVerification = Fail(
            Exception("Random error")
        )
    )

    private fun payload() = Success(
        NetworkingSaveToLinkVerificationState.Payload(
            email = "theLargestEmailYoulleverseeThatCouldBreakALayout@email.com",
            phoneNumber = "12345678",
            otpElement = OTPElement(
                IdentifierSpec.Generic("otp"),
                OTPController()
            ),
            consumerSessionClientSecret = "12345678"
        )
    )

    private fun loading() = NetworkingSaveToLinkVerificationState(
        payload = Loading(),
    )
}
