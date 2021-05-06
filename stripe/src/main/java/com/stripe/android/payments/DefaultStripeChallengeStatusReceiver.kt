package com.stripe.android.payments

import com.stripe.android.Logger
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.StripeException
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.RetryDelaySupplier
import com.stripe.android.networking.StripeRepository
import com.stripe.android.stripe3ds2.transaction.ChallengeFlowOutcome
import com.stripe.android.stripe3ds2.transaction.ChallengeStatusReceiver
import com.stripe.android.stripe3ds2.transaction.CompletionEvent
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.stripe3ds2.transaction.StripeChallengeStatusReceiver
import com.stripe.android.stripe3ds2.transaction.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * The default implementation of [StripeChallengeStatusReceiver]. Used to receive a result from
 * the 3DS2 challenge flow. See [StripeChallengeStatusReceiver] and [ChallengeStatusReceiver] for
 * more details.
 */
internal class DefaultStripeChallengeStatusReceiver internal constructor(
    private val stripe3ds2CompletionStarter: Stripe3ds2CompletionStarter,
    private val stripeRepository: StripeRepository,
    private val stripeIntent: StripeIntent,
    private val sourceId: String,
    private val requestOptions: ApiRequest.Options,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val transaction: Transaction,
    private val onEndChallenge: () -> Unit,
    private val retryDelaySupplier: RetryDelaySupplier = RetryDelaySupplier(),
    enableLogging: Boolean = false,
    private val workContext: CoroutineContext
) : StripeChallengeStatusReceiver() {
    private val logger = Logger.getInstance(enableLogging)

    override fun completed(
        completionEvent: CompletionEvent,
        uiTypeCode: String,
        flowOutcome: ChallengeFlowOutcome
    ) {
        super.completed(completionEvent, uiTypeCode, flowOutcome)
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create3ds2Challenge(
                AnalyticsEvent.Auth3ds2ChallengeCompleted,
                uiTypeCode
            )
        )
        log3ds2ChallengePresented()

        complete3ds2Auth(flowOutcome)
    }

    override fun cancelled(
        uiTypeCode: String
    ) {
        super.cancelled(uiTypeCode)
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create3ds2Challenge(
                AnalyticsEvent.Auth3ds2ChallengeCanceled,
                uiTypeCode
            )
        )
        log3ds2ChallengePresented()

        complete3ds2Auth(ChallengeFlowOutcome.Cancel)
    }

    override fun timedout(
        uiTypeCode: String
    ) {
        super.timedout(uiTypeCode)
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create3ds2Challenge(
                AnalyticsEvent.Auth3ds2ChallengeTimedOut,
                uiTypeCode
            )
        )
        log3ds2ChallengePresented()

        complete3ds2Auth(ChallengeFlowOutcome.Timeout)
    }

    override fun protocolError(
        protocolErrorEvent: ProtocolErrorEvent
    ) {
        super.protocolError(protocolErrorEvent)
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(AnalyticsEvent.Auth3ds2ChallengeErrored)
        )
        log3ds2ChallengePresented()

        complete3ds2Auth(ChallengeFlowOutcome.ProtocolError)
    }

    override fun runtimeError(
        runtimeErrorEvent: RuntimeErrorEvent
    ) {
        super.runtimeError(runtimeErrorEvent)
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(AnalyticsEvent.Auth3ds2ChallengeErrored)
        )
        log3ds2ChallengePresented()

        complete3ds2Auth(ChallengeFlowOutcome.RuntimeError)
    }

    private fun log3ds2ChallengePresented() {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create3ds2Challenge(
                AnalyticsEvent.Auth3ds2ChallengePresented,
                transaction.initialChallengeUiType.orEmpty()
            )
        )
    }

    /**
     * Call [StripeRepository.complete3ds2Auth] to notify the Stripe API that the 3DS2
     * challenge has been completed.
     *
     * When successful, call [startCompletionActivity] to return the result.
     *
     * When [StripeRepository.complete3ds2Auth] fails, handle in [onComplete3ds2AuthFailure].
     *
     * @param flowOutcome the outcome of the 3DS2 challenge flow.
     * @param remainingRetries the number of retry attempts remaining. Defaults to [MAX_RETRIES].
     */
    private fun complete3ds2Auth(
        flowOutcome: ChallengeFlowOutcome,
        remainingRetries: Int = MAX_RETRIES,
    ) {
        CoroutineScope(workContext).launch {
            // ignore result
            runCatching {
                stripeRepository.complete3ds2Auth(
                    sourceId,
                    requestOptions
                )
            }.fold(
                onSuccess = {
                    val attemptedRetries = MAX_RETRIES - remainingRetries
                    logger.debug(
                        "3DS2 challenge completion request was successful. " +
                            "$attemptedRetries retries attempted."
                    )
                    startCompletionActivity(flowOutcome)
                },
                onFailure = { error ->
                    onComplete3ds2AuthFailure(
                        flowOutcome, remainingRetries, error
                    )
                }
            )
        }
    }

    /**
     * When [StripeRepository.complete3ds2Auth] fails with a client error (a 4xx status code)
     * and [remainingRetries] is greater than 0, retry after a delay.
     *
     * The delay logic can be found in [RetryDelaySupplier.getDelayMillis].
     *
     * @param flowOutcome the outcome of the 3DS2 challenge flow.
     * @param remainingRetries the number of retry attempts remaining. Defaults to [MAX_RETRIES].
     */
    private suspend fun onComplete3ds2AuthFailure(
        flowOutcome: ChallengeFlowOutcome,
        remainingRetries: Int,
        error: Throwable,
    ) {
        logger.error(
            "3DS2 challenge completion request failed. Remaining retries: $remainingRetries",
            error
        )

        val isClientError = when (error) {
            is StripeException -> error.isClientError
            else -> false
        }
        val shouldRetry = remainingRetries > 0 && isClientError

        if (shouldRetry) {
            delay(
                retryDelaySupplier.getDelayMillis(
                    MAX_RETRIES,
                    remainingRetries
                )
            )

            // attempt request with a decremented `retries`
            complete3ds2Auth(
                flowOutcome,
                remainingRetries = remainingRetries - 1
            )
        } else {
            logger.debug(
                "Did not make a successful 3DS2 challenge completion request after retrying."
            )
            // There's nothing left to do, complete.
            startCompletionActivity(flowOutcome)
        }
    }

    private suspend fun startCompletionActivity(
        flowOutcome: ChallengeFlowOutcome
    ) = withContext(Dispatchers.Main) {
        onEndChallenge()

        stripe3ds2CompletionStarter.start(
            PaymentFlowResult.Unvalidated(
                clientSecret = stripeIntent.clientSecret.orEmpty(),
                stripeAccountId = requestOptions.stripeAccount,
                flowOutcome = when (flowOutcome) {
                    ChallengeFlowOutcome.CompleteSuccessful ->
                        StripeIntentResult.Outcome.SUCCEEDED
                    ChallengeFlowOutcome.Cancel ->
                        StripeIntentResult.Outcome.CANCELED
                    ChallengeFlowOutcome.Timeout ->
                        StripeIntentResult.Outcome.TIMEDOUT
                    ChallengeFlowOutcome.CompleteUnsuccessful,
                    ChallengeFlowOutcome.ProtocolError,
                    ChallengeFlowOutcome.RuntimeError ->
                        StripeIntentResult.Outcome.FAILED
                    else -> StripeIntentResult.Outcome.UNKNOWN
                }
            )
        )
    }

    private companion object {
        private const val MAX_RETRIES = 3
    }
}
