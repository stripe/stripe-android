package com.stripe.android.stripe3ds2.transaction

import java.security.KeyPair
import java.security.PublicKey

internal class StripeTransaction(
    private val areqParamsFactory: AuthenticationRequestParametersFactory,
    private val directoryServerId: String,
    private val directoryServerPublicKey: PublicKey,
    private val directoryServerKeyId: String?,
    override val sdkTransactionId: SdkTransactionId,
    private val sdkKeyPair: KeyPair,
    private val sdkReferenceNumber: String
) : Transaction {
    override suspend fun createAuthenticationRequestParameters(): AuthenticationRequestParameters {
        return areqParamsFactory.create(
            directoryServerId,
            directoryServerPublicKey,
            directoryServerKeyId,
            sdkTransactionId,
            sdkKeyPair.public
        )
    }

    override fun createInitChallengeArgs(
        challengeParameters: ChallengeParameters,
        timeoutMins: Int,
        intentData: IntentData
    ): InitChallengeArgs {
        return InitChallengeArgs(
            sdkReferenceNumber,
            sdkKeyPair,
            challengeParameters,
            timeoutMins.coerceAtLeast(MIN_TIMEOUT),
            intentData
        )
    }

    internal companion object {
        internal const val MIN_TIMEOUT = 5
    }
}
