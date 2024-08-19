package com.stripe.android.payments

import android.content.Context
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripeIntentResult.Outcome.Companion.CANCELED
import com.stripe.android.StripeIntentResult.Outcome.Companion.SUCCEEDED
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.MaxRetryReachedException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.LinearRetryDelaySupplier
import com.stripe.android.core.networking.RetryDelaySupplier
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.shouldRefresh
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    private val workContext: CoroutineContext,
    private val retryDelaySupplier: RetryDelaySupplier = LinearRetryDelaySupplier()
) {
    private val failureMessageFactory = PaymentFlowFailureMessageFactory(context)

    suspend fun processResult(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): Result<S> = withContext(workContext) {
        val result = runCatching { unvalidatedResult.validate() }.getOrElse {
            return@withContext Result.failure(it)
        }

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKeyProvider.get(),
            stripeAccount = result.stripeAccountId
        )

        retrieveStripeIntent(
            clientSecret = result.clientSecret,
            requestOptions = requestOptions,
            expandFields = EXPAND_PAYMENT_METHOD
        ).mapCatching { stripeIntent ->
            when {
                stripeIntent.status == StripeIntent.Status.Succeeded ||
                    stripeIntent.status == StripeIntent.Status.RequiresCapture -> {
                    createStripeIntentResult(
                        stripeIntent,
                        SUCCEEDED,
                        failureMessageFactory.create(stripeIntent, result.flowOutcome)
                    )
                }
                shouldRefreshIntent(stripeIntent, result.flowOutcome) -> {
                    val intent = refreshStripeIntentUntilTerminalState(
                        stripeIntent,
                        result.clientSecret,
                        requestOptions
                    ).getOrThrow()
                    val flowOutcome = determineFlowOutcome(intent, result.flowOutcome)
                    createStripeIntentResult(
                        intent,
                        flowOutcome,
                        failureMessageFactory.create(intent, result.flowOutcome)
                    )
                }
                shouldCancelIntentSource(stripeIntent, result.canCancelSource) -> {
                    val sourceId = result.sourceId.orEmpty()
                    logger.debug(
                        "Canceling source '$sourceId' for '${stripeIntent.javaClass.simpleName}'"
                    )

                    // When the NextActionData is Use3DS2 and has non-null threeDs2IntentId and
                    // publishableKey, they should be used when calling `source_cancel`
                    val threeDS2Data =
                        stripeIntent.nextActionData as? StripeIntent.NextActionData.SdkData.Use3DS2

                    val intent = cancelStripeIntentSource(
                        stripeIntentId = threeDS2Data?.threeDS2IntentId ?: stripeIntent.id.orEmpty(),
                        requestOptions = threeDS2Data?.publishableKey?.let {
                            ApiRequest.Options(it)
                        } ?: requestOptions,
                        sourceId = sourceId
                    ).getOrThrow()

                    createStripeIntentResult(
                        intent,
                        result.flowOutcome,
                        failureMessageFactory.create(intent, result.flowOutcome)
                    )
                }
                else -> {
                    createStripeIntentResult(
                        stripeIntent,
                        result.flowOutcome,
                        failureMessageFactory.create(stripeIntent, result.flowOutcome)
                    )
                }
            }
        }
    }

    private fun shouldCancelIntentSource(
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
        // For a PaymentIntent with such payment method, we will need to poll the refresh endpoint
        // until the PaymentIntent reaches a deterministic state.
        val succeededMaybeRefresh = flowOutcome == SUCCEEDED && stripeIntent.shouldRefresh()

        // For 3DS flow, if the transaction is still unexpectedly processing, refresh the
        // PaymentIntent. This could happen if, for example, a payment is approved in a WebView,
        // user closes the sheet, and the approval races with this fetch
        val cancelledMaybeRefresh = flowOutcome == CANCELED &&
            stripeIntent.status == StripeIntent.Status.Processing &&
            stripeIntent.paymentMethod?.type == PaymentMethod.Type.Card

        // For similar reasons, the transaction could be unexpectedly stuck in `requires_action` for
        // a UseStripeSDK next_action. If so, refresh the PaymentIntent.
        val actionNotProcessedMaybeRefresh = flowOutcome == CANCELED &&
            stripeIntent.status == StripeIntent.Status.RequiresAction &&
            stripeIntent.paymentMethod?.type == PaymentMethod.Type.Card &&
            stripeIntent.nextActionType == StripeIntent.NextActionType.UseStripeSdk

        // For some payment method types, the intent status can still be `requires_action` by the time the user
        // gets back to the merchant app. We poll until it's succeeded.
        val shouldRefresh = stripeIntent.requiresAction() &&
            stripeIntent.paymentMethod?.type?.afterRedirectAction?.shouldRefresh == true

        return succeededMaybeRefresh || cancelledMaybeRefresh || actionNotProcessedMaybeRefresh || shouldRefresh
    }

    private fun determineFlowOutcome(intent: StripeIntent, originalFlowOutcome: Int): Int {
        return when (intent.status) {
            StripeIntent.Status.Succeeded,
            StripeIntent.Status.RequiresCapture -> SUCCEEDED
            else -> originalFlowOutcome
        }
    }

    /**
     * https://livegrep.corp.stripe.com/view/stripe-internal/pay-server/lib/payment_flows/private/commands/refresh_payment_intent.rb#L23
     * The refresh endpoint will safely send the intent data if it isn't in requires_action,
     * but if it is in requires_action it will try to refresh and fail as refresh is only
     * implemented on wechat_pay and upi
     */
    private fun shouldCallRefreshIntent(stripeIntent: StripeIntent): Boolean {
        return stripeIntent.paymentMethod?.type?.afterRedirectAction is PaymentMethod.AfterRedirectAction.Refresh
    }

    protected abstract suspend fun retrieveStripeIntent(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<T>

    protected abstract suspend fun refreshStripeIntent(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<T>

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
     */
    private suspend fun refreshStripeIntentUntilTerminalState(
        originalIntent: StripeIntent,
        clientSecret: String,
        requestOptions: ApiRequest.Options
    ): Result<T> {
        val maxRetries = originalIntent.paymentMethod?.type?.afterRedirectAction?.retryCount ?: MAX_RETRIES
        var remainingRetries = maxRetries

        var stripeIntentResult = if (shouldCallRefreshIntent(originalIntent)) {
            refreshStripeIntent(
                clientSecret = clientSecret,
                requestOptions = requestOptions,
                expandFields = EXPAND_PAYMENT_METHOD
            )
        } else {
            retrieveStripeIntent(
                clientSecret = clientSecret,
                requestOptions = requestOptions,
                expandFields = EXPAND_PAYMENT_METHOD
            )
        }

        withTimeoutOrNull(retryDelaySupplier.maxDuration(maxRetries = maxRetries)) {
            while (shouldRetry(stripeIntentResult) && remainingRetries > 1) {
                val delayDuration = retryDelaySupplier.getDelay(
                    maxRetries,
                    remainingRetries
                )
                delay(delayDuration)
                stripeIntentResult = if (shouldCallRefreshIntent(originalIntent)) {
                    refreshStripeIntent(
                        clientSecret = clientSecret,
                        requestOptions = requestOptions,
                        expandFields = EXPAND_PAYMENT_METHOD
                    )
                } else {
                    retrieveStripeIntent(
                        clientSecret = clientSecret,
                        requestOptions = requestOptions,
                        expandFields = EXPAND_PAYMENT_METHOD
                    )
                }
                remainingRetries--
            }
        }

        return stripeIntentResult
    }

    /**
     * Cancels the source of this intent so that the payment method attached to it is cleared,
     * transferring the intent's status from requires_action to requires_payment_method.
     */
    protected abstract suspend fun cancelStripeIntentSource(
        stripeIntentId: String,
        requestOptions: ApiRequest.Options,
        sourceId: String
    ): Result<T>

    protected abstract fun createStripeIntentResult(
        stripeIntent: T,
        @StripeIntentResult.Outcome outcomeFromFlow: Int,
        failureMessage: String?
    ): S

    private fun shouldRetry(stripeIntentResult: Result<StripeIntent>): Boolean {
        val stripeIntent = stripeIntentResult.getOrNull() ?: return true
        val requiresAction = stripeIntent.requiresAction()
        val isCardPaymentProcessing = stripeIntent.status == StripeIntent.Status.Processing &&
            stripeIntent.paymentMethod?.type == PaymentMethod.Type.Card
        return requiresAction || isCardPaymentProcessing
    }

    internal companion object {
        val EXPAND_PAYMENT_METHOD = listOf("payment_method")
        const val MAX_RETRIES = 5
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
    @IOContext workContext: CoroutineContext
) : PaymentFlowResultProcessor<PaymentIntent, PaymentIntentResult>(
    context,
    publishableKeyProvider,
    stripeRepository,
    logger,
    workContext
) {
    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        return stripeRepository.retrievePaymentIntent(
            clientSecret,
            requestOptions,
            expandFields,
        )
    }

    override suspend fun refreshStripeIntent(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        return stripeRepository.refreshPaymentIntent(
            clientSecret,
            requestOptions,
        )
    }

    override suspend fun cancelStripeIntentSource(
        stripeIntentId: String,
        requestOptions: ApiRequest.Options,
        sourceId: String
    ): Result<PaymentIntent> {
        return stripeRepository.cancelPaymentIntentSource(
            paymentIntentId = stripeIntentId,
            sourceId = sourceId,
            options = requestOptions,
        )
    }

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
    context,
    publishableKeyProvider,
    stripeRepository,
    logger,
    workContext
) {
    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        return stripeRepository.retrieveSetupIntent(
            clientSecret,
            requestOptions,
            expandFields
        )
    }

    override suspend fun refreshStripeIntent(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        return stripeRepository.refreshSetupIntent(
            clientSecret,
            requestOptions,
        )
    }

    override suspend fun cancelStripeIntentSource(
        stripeIntentId: String,
        requestOptions: ApiRequest.Options,
        sourceId: String
    ): Result<SetupIntent> {
        return stripeRepository.cancelSetupIntentSource(
            setupIntentId = stripeIntentId,
            sourceId = sourceId,
            options = requestOptions,
        )
    }

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
