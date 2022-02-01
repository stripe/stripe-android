package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.security.KeyPair

@Parcelize
data class InitChallengeArgs(
    internal val sdkReferenceNumber: String,
    internal val sdkKeyPair: KeyPair,
    internal val challengeParameters: ChallengeParameters,
    internal val timeoutMins: Int,
    internal val intentData: IntentData
) : Parcelable
