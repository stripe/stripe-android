package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Handles user actions from the challenge screen.
 *
 * Responsible for
 * 1. Creating new CReq payload based on user entry
 * 2. Firing challenge request
 * 3. Handling challenge response
 */
internal interface ChallengeActionHandler {
    /**
     * Call when a user takes on action on the challenge screen.
     *
     * @param action the action that the user took
     */
    suspend fun submit(action: ChallengeAction): ChallengeRequestResult

    class Default internal constructor(
        private val creqData: ChallengeRequestData,
        private val errorReporter: ErrorReporter,
        private val challengeRequestExecutor: ChallengeRequestExecutor,
        private val workContext: CoroutineContext
    ) : ChallengeActionHandler {
        constructor(
            creqData: ChallengeRequestData,
            errorReporter: ErrorReporter,
            creqExecutorFactory: ChallengeRequestExecutor.Factory,
            workContext: CoroutineContext
        ) : this(
            creqData,
            errorReporter,
            creqExecutorFactory.create(errorReporter, workContext),
            workContext
        )

        override suspend fun submit(
            action: ChallengeAction
        ): ChallengeRequestResult = withContext(workContext) {
            val creqData = ChallengeRequestData(
                messageVersion = creqData.messageVersion,
                threeDsServerTransId = creqData.threeDsServerTransId,
                acsTransId = creqData.acsTransId,
                sdkTransId = creqData.sdkTransId,
                messageExtensions = creqData.messageExtensions,
                threeDSRequestorAppURL = creqData.threeDSRequestorAppURL
            ).let {
                when (action) {
                    is ChallengeAction.NativeForm -> {
                        it.copy(challengeDataEntry = action.userEntry, whitelistingDataEntry = action.whitelistingValue)
                    }
                    is ChallengeAction.HtmlForm -> {
                        it.copy(challengeHtmlDataEntry = action.userEntry)
                    }
                    is ChallengeAction.Oob -> {
                        it.copy(oobContinue = true, whitelistingDataEntry = action.whitelistingValue)
                    }
                    is ChallengeAction.Resend -> {
                        it.copy(shouldResendChallenge = true)
                    }
                    is ChallengeAction.Cancel -> {
                        it.copy(cancelReason = ChallengeRequestData.CancelReason.UserSelected)
                    }
                }
            }
            executeChallengeRequest(creqData)
        }

        private suspend fun executeChallengeRequest(
            creqData: ChallengeRequestData
        ): ChallengeRequestResult {
            delay(CREQ_DELAY)

            return runCatching {
                challengeRequestExecutor.execute(creqData)
            }.onFailure {
                errorReporter.reportError(
                    RuntimeException(
                        """
                            Failed to execute challenge request.

                            CReq = ${creqData.sanitize()}
                        """.trimIndent(),
                        it
                    )
                )
            }.getOrElse {
                ChallengeRequestResult.RuntimeError(it)
            }
        }

        internal companion object {
            internal val CREQ_DELAY = TimeUnit.SECONDS.toMillis(1)
        }
    }
}
