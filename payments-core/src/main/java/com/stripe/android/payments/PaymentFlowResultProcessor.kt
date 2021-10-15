package com.stripe.android.payments

import android.content.Context
import com.stripe.android.Logger
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripeIntentResult.Outcome.Companion.SUCCEEDED
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.MaxRetryReachedException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.shouldRefresh
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.RetryDelaySupplier
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Class responsible for processing the result of a [PaymentController] confirm operation.
 */
internal sealed class PaymentFlowResultProcessor<T : StripeIntent, out S : StripeIntentResult<T>>(
    context: Context,
    private val publishableKeyProvider: Provider<String>,
    protected val stripeRepository: StripeRepository,
    private val logger: Logger,
    private val workContext: CoroutineContext
) {
    private val failureMessageFactory = PaymentFlowFailureMessageFactory(context)

    suspend fun processResult(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): S = withContext(workContext) {
        val result = unvalidatedResult.validate()

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKeyProvider.get(),
            stripeAccount = result.stripeAccountId
        )

        requireNotNull(
            retrieveStripeIntent(
                result.clientSecret,
                requestOptions,
                expandFields = EXPAND_PAYMENT_METHOD
            )
        ).let { stripeIntent ->
            when {
                shouldRefreshIntent(stripeIntent, result.flowOutcome) -> {
                    refreshStripeIntentTillTerminalState(
                        result.clientSecret,
                        requestOptions
                    )
                }
                shouldCancelIntent(stripeIntent, result.canCancelSource) -> {
                    val sourceId = result.sourceId.orEmpty()
                    logger.debug(
                        "Canceling source '$sourceId' for '${stripeIntent.javaClass.simpleName}'"
                    )

                    requireNotNull(
                        cancelStripeIntent(
                            stripeIntent,
                            requestOptions,
                            sourceId,
                        )
                    )
                }
                else -> {
                    stripeIntent
                }
            }
        }.let { stripeIntent ->
            createStripeIntentResult(
                stripeIntent,
                result.flowOutcome,
                failureMessageFactory.create(stripeIntent, result.flowOutcome)
            )
        }
    }

    private fun shouldCancelIntent(
        stripeIntent: StripeIntent,
        shouldCancelSource: Boolean
    ): Boolean {
        // It is very important to check `requiresAction()` because we can't always tell what
        // action the customer took during payment authentication (e.g. when using Custom Tabs).
        // We don't want to cancel if required actions have been resolved and the payment is ready
        // for capture.
        return shouldCancelSource && stripeIntent.requiresAction()
    }

    private fun shouldRefreshIntent(
        stripeIntent: StripeIntent,
        @StripeIntentResult.Outcome flowOutcome: Int
    ): Boolean {
        // For some payment methods, after user confirmation(resulting in flowOutCome == SUCCEEDED),
        // there is a delay when Stripe backend transfers its state out of "requires_action".
        // The a PaymentIntent with such payment method, we will need to poll the refresh endpoint
        // until the PaymentIntent reaches a deterministic state.
        return flowOutcome == SUCCEEDED && stripeIntent.shouldRefresh()
    }

    protected abstract suspend fun retrieveStripeIntent(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): T?

    /**
     * Keeps polling refresh endpoint for this [StripeIntent] until its status is no longer
     * "requires_action".
     *
     * @param clientSecret for the intent
     * @param requestOptions options for [ApiRequest]
     *
     * @return a [StripeIntent] object with a deterministic state.
     *
     * @throws MaxRetryReachedException when max retry is reached and the status is still
     * "requires_action".
     * @throws InvalidRequestException if the intent is a [SetupIntent], refresh endpoint is only
     * available for [PaymentIntent].
     */
    @Throws(
        MaxRetryReachedException::class,
        InvalidRequestException::class
    )
    protected abstract suspend fun refreshStripeIntentTillTerminalState(
        clientSecret: String,
        requestOptions: ApiRequest.Options
    ): T

    protected abstract suspend fun cancelStripeIntent(
        stripeIntent: T,
        requestOptions: ApiRequest.Options,
        sourceId: String
    ): T?

    protected abstract fun createStripeIntentResult(
        stripeIntent: T,
        @StripeIntentResult.Outcome outcomeFromFlow: Int,
        failureMessage: String?
    ): S

    internal companion object {
        val EXPAND_PAYMENT_METHOD = listOf("payment_method")
    }
}

/**
 * Processes the result of a [PaymentIntent] confirmation.
 */
@Singleton
internal class PaymentIntentFlowResultProcessor @Inject constructor(
    context: Context,
    @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
    stripeRepository: StripeRepository,
    logger: Logger,
    @IOContext workContext: CoroutineContext,
    val retryDelaySupplier: RetryDelaySupplier
) : PaymentFlowResultProcessor<PaymentIntent, PaymentIntentResult>(
    context, publishableKeyProvider, stripeRepository, logger, workContext
) {
    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): PaymentIntent? =
        stripeRepository.retrievePaymentIntent(
            clientSecret,
            requestOptions,
            expandFields
        )

    override suspend fun refreshStripeIntentTillTerminalState(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
    ): PaymentIntent {
        var remainingRetries = MAX_RETRIES - 1

        var stripeIntent = requireNotNull(
            stripeRepository.refreshPaymentIntent(
                clientSecret,
                requestOptions
            )
        )
        while (stripeIntent.requiresAction() && remainingRetries > 0) {
            val delayMs = retryDelaySupplier.getDelayMillis(
                3,
                remainingRetries
            )
            delay(delayMs)
            stripeIntent = requireNotNull(
                stripeRepository.refreshPaymentIntent(
                    clientSecret,
                    requestOptions
                )
            )
            remainingRetries--
        }

        if (stripeIntent.requiresAction()) {
            throw MaxRetryReachedException()
        } else {
            return stripeIntent
        }
    }

    override suspend fun cancelStripeIntent(
        stripeIntent: PaymentIntent,
        requestOptions: ApiRequest.Options,
        sourceId: String
    ): PaymentIntent? =
        stripeRepository.cancelPaymentIntentSource(
            stripeIntent.id.orEmpty(),
            sourceId,
            requestOptions,
        )

    override fun createStripeIntentResult(
        stripeIntent: PaymentIntent,
        outcomeFromFlow: Int,
        failureMessage: String?
    ): PaymentIntentResult =
        PaymentIntentResult(
            stripeIntent,
            outcomeFromFlow,
            failureMessage
        )

    internal companion object {
        const val MAX_RETRIES = 3
    }
}

/**
 * Processes the result of a [SetupIntent] confirmation.
 */
@Singleton
internal class SetupIntentFlowResultProcessor @Inject constructor(
    context: Context,
    @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
    stripeRepository: StripeRepository,
    logger: Logger,
    @IOContext workContext: CoroutineContext
) : PaymentFlowResultProcessor<SetupIntent, SetupIntentResult>(
    context, publishableKeyProvider, stripeRepository, logger, workContext
) {
    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): SetupIntent? =
        stripeRepository.retrieveSetupIntent(
            clientSecret,
            requestOptions,
            expandFields
        )

    override suspend fun refreshStripeIntentTillTerminalState(
        clientSecret: String,
        requestOptions: ApiRequest.Options
    ): SetupIntent {
        throw InvalidRequestException(
            message = "refresh endpoint is not available for SetupIntent. " +
                "client_secret: $clientSecret"
        )
    }

    override suspend fun cancelStripeIntent(
        stripeIntent: SetupIntent,
        requestOptions: ApiRequest.Options,
        sourceId: String
    ): SetupIntent? =
        stripeRepository.cancelSetupIntentSource(
            stripeIntent.id.orEmpty(),
            sourceId,
            requestOptions,
        )

    override fun createStripeIntentResult(
        stripeIntent: SetupIntent,
        outcomeFromFlow: Int,
        failureMessage: String?
    ): SetupIntentResult =
        SetupIntentResult(
            stripeIntent,
            outcomeFromFlow,
            failureMessage
        )
}
