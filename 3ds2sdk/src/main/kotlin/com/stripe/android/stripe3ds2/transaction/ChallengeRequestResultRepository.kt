package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import kotlin.coroutines.CoroutineContext

internal interface ChallengeRequestResultRepository {
    suspend fun get(
        creqExecutorConfig: ChallengeRequestExecutor.Config,
        challengeRequestData: ChallengeRequestData
    ): ChallengeRequestResult
}

internal class DefaultChallengeRequestResultRepository(
    private val errorReporter: ErrorReporter,
    private val workContext: CoroutineContext
) : ChallengeRequestResultRepository {
    override suspend fun get(
        creqExecutorConfig: ChallengeRequestExecutor.Config,
        challengeRequestData: ChallengeRequestData
    ): ChallengeRequestResult {
        return StripeChallengeRequestExecutor.Factory(creqExecutorConfig)
            .create(errorReporter, workContext)
            .execute(challengeRequestData)
    }
}
