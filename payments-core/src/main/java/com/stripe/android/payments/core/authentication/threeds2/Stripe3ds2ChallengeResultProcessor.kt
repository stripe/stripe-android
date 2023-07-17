package com.stripe.android.payments.core.authentication.threeds2

import com.stripe.android.StripeIntentResult
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.RetryDelaySupplier
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal interface Stripe3ds2ChallengeResultProcessor {
    suspend fun process(challengeResult: ChallengeResult): PaymentFlowResult.Unvalidated
}

@Singleton
internal class DefaultStripe3ds2ChallengeResultProcessor @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    private val retryDelaySupplier: RetryDelaySupplier,
    private val logger: Logger,
    @IOContext private val workContext: CoroutineContext
) : Stripe3ds2ChallengeResultProcessor {

    override suspend fun process(
        challengeResult: ChallengeResult
    ): PaymentFlowResult.Unvalidated = withContext(workContext) {
        when (challengeResult) {
            is ChallengeResult.Succeeded -> {
                analyticsRequestExecutor.executeAsync(
                    paymentAnalyticsRequestFactory.create3ds2Challenge(
                        PaymentAnalyticsEvent.Auth3ds2ChallengeCompleted,
                        challengeResult.uiTypeCode
                    )
                )
            }
            is ChallengeResult.Failed -> {
                analyticsRequestExecutor.executeAsync(
                    paymentAnalyticsRequestFactory.create3ds2Challenge(
                        PaymentAnalyticsEvent.Auth3ds2ChallengeCompleted,
                        challengeResult.uiTypeCode
                    )
                )
            }
            is ChallengeResult.Canceled -> {
                analyticsRequestExecutor.executeAsync(
                    paymentAnalyticsRequestFactory.create3ds2Challenge(
                        PaymentAnalyticsEvent.Auth3ds2ChallengeCanceled,
                        challengeResult.uiTypeCode
                    )
                )
            }
            is ChallengeResult.ProtocolError -> {
                analyticsRequestExecutor.executeAsync(
                    paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.Auth3ds2ChallengeErrored)
                )
            }
            is ChallengeResult.RuntimeError -> {
                analyticsRequestExecutor.executeAsync(
                    paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.Auth3ds2ChallengeErrored)
                )
            }
            is ChallengeResult.Timeout -> {
                analyticsRequestExecutor.executeAsync(
                    paymentAnalyticsRequestFactory.create3ds2Challenge(
                        PaymentAnalyticsEvent.Auth3ds2ChallengeTimedOut,
                        challengeResult.uiTypeCode
                    )
                )
            }
        }

        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.create3ds2Challenge(
                PaymentAnalyticsEvent.Auth3ds2ChallengePresented,
                challengeResult.initialUiType?.code.orEmpty()
            )
        )

        val requestOptions = ApiRequest.Options(
            challengeResult.intentData.publishableKey,
            challengeResult.intentData.accountId
        )

        val completionSucceeded = complete3ds2Auth(challengeResult, requestOptions)
        val flowOutcome = if (completionSucceeded) {
            when (challengeResult) {
                is ChallengeResult.Succeeded -> {
                    StripeIntentResult.Outcome.SUCCEEDED
                }
                is ChallengeResult.Failed -> {
                    StripeIntentResult.Outcome.FAILED
                }
                is ChallengeResult.Canceled -> {
                    StripeIntentResult.Outcome.CANCELED
                }
                is ChallengeResult.ProtocolError -> {
                    StripeIntentResult.Outcome.FAILED
                }
                is ChallengeResult.RuntimeError -> {
                    StripeIntentResult.Outcome.FAILED
                }
                is ChallengeResult.Timeout -> {
                    StripeIntentResult.Outcome.TIMEDOUT
                }
            }
        } else {
            StripeIntentResult.Outcome.FAILED
        }

        PaymentFlowResult.Unvalidated(
            clientSecret = challengeResult.intentData.clientSecret,
            stripeAccountId = requestOptions.stripeAccount,
            flowOutcome = flowOutcome
        )
    }

    /**
     * Call [StripeRepository.complete3ds2Auth] to notify the Stripe API that the 3DS2
     * challenge has been completed.
     *
     * When [StripeRepository.complete3ds2Auth] fails, handle in [onComplete3ds2AuthFailure].
     *
     * @param remainingRetries the number of retry attempts remaining. Defaults to [MAX_RETRIES].
     *
     * @return `true` if [StripeRepository.complete3ds2Auth] was called successfully;
     * `false` otherwise
     */
    private suspend fun complete3ds2Auth(
        challengeResult: ChallengeResult,
        requestOptions: ApiRequest.Options,
        remainingRetries: Int = MAX_RETRIES
    ): Boolean {
        // complete3ds2Auth() result can be ignored
        return stripeRepository.complete3ds2Auth(
            challengeResult.intentData.sourceId,
            requestOptions
        ).fold(
            onSuccess = {
                val attemptedRetries = MAX_RETRIES - remainingRetries
                logger.debug(
                    "3DS2 challenge completion request was successful. " +
                        "$attemptedRetries retries attempted."
                )

                // success
                true
            },
            onFailure = { error ->
                onComplete3ds2AuthFailure(
                    challengeResult,
                    requestOptions,
                    remainingRetries,
                    error
                )
            }
        )
    }

    /**
     * When [StripeRepository.complete3ds2Auth] fails with a client error (a 4xx status code)
     * and [remainingRetries] is greater than 0, retry after a delay.
     * After [remainingRetries] are exhausted, stop attempts.
     *
     * The delay logic can be found in [RetryDelaySupplier.getDelayMillis].
     *
     * @param challengeResult the result of the 3DS2 challenge flow.
     * @param remainingRetries the number of retry attempts remaining. Defaults to [MAX_RETRIES].
     */
    private suspend fun onComplete3ds2AuthFailure(
        challengeResult: ChallengeResult,
        requestOptions: ApiRequest.Options,
        remainingRetries: Int,
        error: Throwable
    ): Boolean {
        logger.error(
            "3DS2 challenge completion request failed. Remaining retries: $remainingRetries",
            error
        )

        val isClientError = when (error) {
            is StripeException -> error.isClientError
            else -> false
        }
        val shouldRetry = remainingRetries > 0 && isClientError

        return if (shouldRetry) {
            delay(
                retryDelaySupplier.getDelayMillis(
                    MAX_RETRIES,
                    remainingRetries
                )
            )

            // attempt request with a decremented `retries`
            complete3ds2Auth(
                challengeResult,
                requestOptions,
                remainingRetries = remainingRetries - 1
            )
        } else {
            logger.debug(
                "Did not make a successful 3DS2 challenge completion request after retrying."
            )

            // failure
            false
        }
    }

    private companion object {
        private const val MAX_RETRIES = 3
    }
}
