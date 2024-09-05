package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.ChallengeMessageFixtures

internal object ChallengeRequestResultFixures {
    val SUCCESS = ChallengeRequestResult.Success(
        ChallengeMessageFixtures.CREQ,
        ChallengeMessageFixtures.CRES,
        ChallengeRequestExecutorFixtures.CONFIG
    )
}
