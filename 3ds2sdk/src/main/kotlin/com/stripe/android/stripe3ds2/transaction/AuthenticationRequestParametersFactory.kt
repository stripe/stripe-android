package com.stripe.android.stripe3ds2.transaction

import java.security.PublicKey

internal interface AuthenticationRequestParametersFactory {
    suspend fun create(
        directoryServerId: String,
        directoryServerPublicKey: PublicKey,
        keyId: String?,
        sdkTransactionId: SdkTransactionId,
        sdkPublicKey: PublicKey
    ): AuthenticationRequestParameters
}
