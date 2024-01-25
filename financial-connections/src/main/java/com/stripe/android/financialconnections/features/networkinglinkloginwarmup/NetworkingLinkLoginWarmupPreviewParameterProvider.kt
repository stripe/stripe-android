package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized

internal class NetworkingLinkLoginWarmupPreviewParameterProvider :
    PreviewParameterProvider<NetworkingLinkLoginWarmupState> {
    override val values = sequenceOf(
        canonical(),
        loading(),
        disablingNetworking(),
        payloadError(),
        disablingError()
    )

    private fun canonical() = NetworkingLinkLoginWarmupState(
        payload = Success(
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = "Test",
                email = "email@test.com"
            )
        ),
        disableNetworkingAsync = Uninitialized
    )

    private fun loading() = NetworkingLinkLoginWarmupState(
        payload = Loading(),
        disableNetworkingAsync = Uninitialized
    )

    private fun payloadError() = NetworkingLinkLoginWarmupState(
        payload = Fail(Exception("Error")),
        disableNetworkingAsync = Uninitialized
    )

    private fun disablingError() = NetworkingLinkLoginWarmupState(
        payload = Success(
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = "Test",
                email = "email@test.com"
            )
        ),
        disableNetworkingAsync = Fail(Exception("Error"))
    )

    private fun disablingNetworking() = NetworkingLinkLoginWarmupState(
        payload = Success(
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = "Test",
                email = "email@test.com"
            )
        ),
        disableNetworkingAsync = Loading()
    )
}
