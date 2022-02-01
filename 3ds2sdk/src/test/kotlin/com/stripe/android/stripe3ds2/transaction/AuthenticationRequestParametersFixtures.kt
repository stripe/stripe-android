package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import java.util.UUID

internal object AuthenticationRequestParametersFixtures {
    val DEFAULT = AuthenticationRequestParameters(
        deviceData = UUID.randomUUID().toString(),
        sdkTransactionId = ChallengeMessageFixtures.SDK_TRANS_ID,
        sdkAppId = UUID.randomUUID().toString(),
        sdkReferenceNumber = UUID.randomUUID().toString(),
        sdkEphemeralPublicKey = UUID.randomUUID().toString(),
        messageVersion = "2.1.0"
    )
}
