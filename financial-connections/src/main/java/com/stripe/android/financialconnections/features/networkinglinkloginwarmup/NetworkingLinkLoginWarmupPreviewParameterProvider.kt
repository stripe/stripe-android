package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.model.LinkBrand

internal class NetworkingLinkLoginWarmupPreviewParameterProvider :
    PreviewParameterProvider<NetworkingLinkLoginWarmupState> {
    override val values = sequenceOf(
        canonical(),
        loading(),
        disablingNetworking(),
        payloadError(),
        disablingError(),
        instantDebits(),
    )

    private fun canonical() = NetworkingLinkLoginWarmupState(
        payload = Success(
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = "Test",
                redactedEmail = "emai•••@test.com",
                email = "email@test.com",
                verifiedFlow = false,
                sessionId = "sessionId"
            )
        ),
        disableNetworkingAsync = Uninitialized,
        isInstantDebits = false,
        linkBrand = LinkBrand.Link,
    )

    private fun loading() = NetworkingLinkLoginWarmupState(
        payload = Loading(),
        disableNetworkingAsync = Uninitialized,
        isInstantDebits = false,
        linkBrand = LinkBrand.Link,
    )

    private fun payloadError() = NetworkingLinkLoginWarmupState(
        payload = Fail(Exception("Error")),
        disableNetworkingAsync = Uninitialized,
        isInstantDebits = false,
        linkBrand = LinkBrand.Link,
    )

    private fun disablingError() = NetworkingLinkLoginWarmupState(
        payload = Success(
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = "Test",
                redactedEmail = "emai•••@test.com",
                email = "email@test.com",
                verifiedFlow = false,
                sessionId = "sessionId"
            )
        ),
        disableNetworkingAsync = Fail(Exception("Error")),
        isInstantDebits = false,
        linkBrand = LinkBrand.Link,
    )

    private fun disablingNetworking() = NetworkingLinkLoginWarmupState(
        payload = Success(
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = "Test",
                redactedEmail = "emai•••@test.com",
                email = "email@test.com",
                verifiedFlow = false,
                sessionId = "sessionId"
            )
        ),
        disableNetworkingAsync = Loading(),
        isInstantDebits = false,
        linkBrand = LinkBrand.Link,
    )

    private fun instantDebits() = NetworkingLinkLoginWarmupState(
        payload = Success(
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = "Test",
                redactedEmail = "emai•••@test.com",
                email = "email@test.com",
                verifiedFlow = false,
                sessionId = "sessionId"
            )
        ),
        disableNetworkingAsync = Uninitialized,
        isInstantDebits = true,
        linkBrand = LinkBrand.Link,
    )
}
