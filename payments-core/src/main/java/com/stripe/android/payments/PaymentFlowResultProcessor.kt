package com.stripe.android.payments

import android.content.Context
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripeIntentResult.Outcome.Companion.CANCELED
import com.stripe.android.StripeIntentResult.Outcome.Companion.FAILED
import com.stripe.android.StripeIntentResult.Outcome.Companion.SUCCEEDED
import com.stripe.android.StripeIntentResult.Outcome.Companion.TIMEDOUT
import com.stripe.android.StripeIntentResult.Outcome.Companion.UNKNOWN
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.ApiRequest
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

        val initialRetrieveIntentStartTime = System.currentTimeMillis()

        val requestId = unvalidatedResult.exception?.requestId

        retrieveStripeIntent(
            clientSecret = result.clientSecret,
            requestOptions = requestOptions,
            expandFields = EXPAND_PAYMENT_METHOD
        ).mapCatching { stripeIntent ->
            when {
                stripeIntent.status == StripeIntent.Status.Succeeded ||
                    stripeIntent.status == StripeIntent.Status.RequiresCapture ||
                    isOrchestrationPayment(stripeIntent, result) -> {
                    val flowOutcome = SUCCEEDED
                    createLoggedStripeIntentResult(
                        stripeIntent = stripeIntent,
                        requestId = requestId,
                        originalFlowOutcome = result.flowOutcome,
                        resolvedFlowOutcome = flowOutcome,
                        failureMessageOutcome = result.flowOutcome,
                        source = "initial_retrieve",
                    )
                }
                shouldRefreshOrPollIntent(stripeIntent, result.flowOutcome) -> {
                    val intent = if (shouldCallRefreshIntent(stripeIntent)) {
                        refreshStripeIntent(
                            clientSecret = result.clientSecret,
                            requestOptions = requestOptions,
                            expandFields = EXPAND_PAYMENT_METHOD
                        ).getOrThrow()
                    } else {
                        pollStripeIntentUntilTerminalState(
                            originalIntent = stripeIntent,
                            clientSecret = result.clientSecret,
                            requestOptions = requestOptions,
                            initialRetrieveIntentStartTime = initialRetrieveIntentStartTime
                        ).getOrThrow()
                    }

                    val flowOutcome = resolvedFlowOutcome(intent, result)
                    createLoggedStripeIntentResult(
                        stripeIntent = intent,
                        requestId = requestId,
                        originalFlowOutcome = result.flowOutcome,
                        resolvedFlowOutcome = flowOutcome,
                        failureMessageOutcome = flowOutcome,
                        source = "refresh_or_poll",
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

                    val flowOutcome = resolvedFlowOutcome(intent, result)
                    createLoggedStripeIntentResult(
                        stripeIntent = intent,
                        requestId = requestId,
                        originalFlowOutcome = result.flowOutcome,
                        resolvedFlowOutcome = flowOutcome,
                        failureMessageOutcome = flowOutcome,
                        source = "cancel_source",
                    )
                }
                else -> {
                    val flowOutcome = resolvedFlowOutcome(stripeIntent, result)
                    createLoggedStripeIntentResult(
                        stripeIntent = stripeIntent,
                        requestId = requestId,
                        originalFlowOutcome = result.flowOutcome,
                        resolvedFlowOutcome = flowOutcome,
                        failureMessageOutcome = flowOutcome,
                        source = "final_retrieve",
                    )
                }
            }
        }
    }

    private fun isOrchestrationPayment(
        stripeIntent: StripeIntent,
        result: PaymentFlowResult.Validated
    ): Boolean = stripeIntent.status == StripeIntent.Status.Processing &&
        stripeIntent.paymentMethod?.type == PaymentMethod.Type.Card &&
        result.flowOutcome != CANCELED

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

    private fun shouldRefreshOrPollIntent(
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

        // Browser-based auth flows return UNKNOWN when the browser hands control back to the app.
        // A card payment can still be racing from `requires_action` / `processing` into its final
        // state at that point, so keep polling until we see the terminal intent status.
        val unknownCardAuthMaybeRefresh = flowOutcome == UNKNOWN &&
            stripeIntent.paymentMethod?.type == PaymentMethod.Type.Card &&
            (
                stripeIntent.status == StripeIntent.Status.Processing ||
                    (
                        stripeIntent.status == StripeIntent.Status.RequiresAction &&
                            (
                                stripeIntent.nextActionType == StripeIntent.NextActionType.UseStripeSdk ||
                                    stripeIntent.nextActionType == StripeIntent.NextActionType.RedirectToUrl
                                )
                        )
                )

        // For some payment method types, the intent status can still be `requires_action` by the time the user
        // gets back to the merchant app. We poll until it's succeeded.
        val shouldRefresh = stripeIntent.requiresAction() &&
            stripeIntent.paymentMethod?.type?.afterRedirectAction?.shouldRefreshOrRetrieve == true

        return succeededMaybeRefresh ||
            cancelledMaybeRefresh ||
            actionNotProcessedMaybeRefresh ||
            unknownCardAuthMaybeRefresh ||
            shouldRefresh
    }

    private fun resolvedFlowOutcome(
        intent: StripeIntent,
        result: PaymentFlowResult.Validated
    ): Int {
        return if (isOrchestrationPayment(intent, result)) {
            SUCCEEDED
        } else {
            determineFlowOutcome(intent, result.flowOutcome)
        }
    }

    private fun determineFlowOutcome(intent: StripeIntent, originalFlowOutcome: Int): Int {
        return when (intent.status) {
            StripeIntent.Status.Succeeded,
            StripeIntent.Status.RequiresCapture -> SUCCEEDED
            StripeIntent.Status.RequiresPaymentMethod -> {
                when (originalFlowOutcome) {
                    CANCELED,
                    FAILED,
                    TIMEDOUT -> originalFlowOutcome
                    else -> FAILED
                }
            }
            StripeIntent.Status.Canceled -> CANCELED
            else -> originalFlowOutcome
        }
    }

    private fun createLoggedStripeIntentResult(
        stripeIntent: T,
        requestId: String?,
        @StripeIntentResult.Outcome originalFlowOutcome: Int,
        @StripeIntentResult.Outcome resolvedFlowOutcome: Int,
        @StripeIntentResult.Outcome failureMessageOutcome: Int,
        source: String,
    ): S {
        val failureMessage = failureMessageFactory.create(
            intent = stripeIntent,
            requestId = requestId,
            outcome = failureMessageOutcome
        )

        logger.info(
            listOfNotNull(
                "PaymentFlowResultProcessor result",
                "source=$source",
                "intentId=${stripeIntent.id}",
                "status=${stripeIntent.status}",
                "initialFlowOutcome=${originalFlowOutcome.toOutcomeString()}",
                "resolvedFlowOutcome=${resolvedFlowOutcome.toOutcomeString()}",
                "nextActionType=${stripeIntent.nextActionType}",
                "paymentMethodType=${stripeIntent.paymentMethod?.type?.code}",
                stripeIntent.lastErrorTypeCode()?.let { "errorType=$it" },
                stripeIntent.lastErrorCode()?.let { "errorCode=$it" },
                stripeIntent.lastDeclineCode()?.let { "declineCode=$it" },
                failureMessage?.let { "failureMessage=$it" },
            ).joinToString(", ")
        )

        return createStripeIntentResult(
            stripeIntent = stripeIntent,
            outcomeFromFlow = resolvedFlowOutcome,
            failureMessage = failureMessage,
        )
    }

    private fun StripeIntent.lastErrorTypeCode(): String? {
        return when (this) {
            is PaymentIntent -> lastPaymentError?.type?.code
            is SetupIntent -> lastSetupError?.type?.code
        }
    }

    private fun StripeIntent.lastErrorCode(): String? {
        return when (this) {
            is PaymentIntent -> lastPaymentError?.code
            is SetupIntent -> lastSetupError?.code
        }
    }

    private fun StripeIntent.lastDeclineCode(): String? {
        return when (this) {
            is PaymentIntent -> lastPaymentError?.declineCode
            is SetupIntent -> lastSetupError?.declineCode
        }
    }

    private fun Int.toOutcomeString(): String {
        return when (this) {
            SUCCEEDED -> "succeeded"
            FAILED -> "failed"
            CANCELED -> "canceled"
            TIMEDOUT -> "timed_out"
            UNKNOWN -> "unknown"
            else -> "unrecognized($this)"
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
     * Keeps polling retrieve endpoint for this [StripeIntent] until its status is no longer
     * "requires_action".
     *
     * @param clientSecret for the intent
     * @param requestOptions options for [ApiRequest]
     * @param initialRetrieveIntentStartTime time in milliseconds that the initial retrieveStripeIntent call was made.
     *
     * @return a [StripeIntent] object with a deterministic state.
     */
    private suspend fun pollStripeIntentUntilTerminalState(
        originalIntent: StripeIntent,
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        initialRetrieveIntentStartTime: Long
    ): Result<T> {
        var timeOfLastRequest = initialRetrieveIntentStartTime
        var stripeIntentResult: Result<T>? = null

        val timeRemaining = getPollingDurationForPaymentMethod(originalIntent) -
            (System.currentTimeMillis() - initialRetrieveIntentStartTime)

        withTimeoutOrNull(timeRemaining) {
            stripeIntentResult = retrieveStripeIntent(clientSecret, requestOptions, EXPAND_PAYMENT_METHOD)
            while (shouldRetry(stripeIntentResult)) {
                // We want to delay a maximum of 1s between requests, including the time the request took.
                // e.g. if the previous request took 250ms, the delay will be 750ms
                delay(POLLING_DELAY - (System.currentTimeMillis() - timeOfLastRequest))
                timeOfLastRequest = System.currentTimeMillis()
                stripeIntentResult = retrieveStripeIntent(clientSecret, requestOptions, EXPAND_PAYMENT_METHOD)
            }
        }

        // Retrieve final time if intent not in terminal state OR result is null which is possible if the initial
        // request took longer than the polling duration for the payment method. Ensures we always call retrieve
        // at least once after the polling duration
        if (shouldRetry(stripeIntentResult) || stripeIntentResult == null) {
            stripeIntentResult = retrieveStripeIntent(clientSecret, requestOptions, EXPAND_PAYMENT_METHOD)
        }

        return stripeIntentResult as Result<T>
    }

    private fun getPollingDurationForPaymentMethod(stripeIntent: StripeIntent): Long {
        return stripeIntent.paymentMethod?.type?.afterRedirectAction?.pollingDuration ?: MAX_POLLING_DURATION
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

    private fun shouldRetry(stripeIntentResult: Result<StripeIntent>?): Boolean {
        val stripeIntent = stripeIntentResult?.getOrNull() ?: return true
        val requiresAction = stripeIntent.requiresAction()
        val isCardPaymentProcessing = stripeIntent.status == StripeIntent.Status.Processing &&
            stripeIntent.paymentMethod?.type == PaymentMethod.Type.Card
        return requiresAction || isCardPaymentProcessing
    }

    internal companion object {
        val EXPAND_PAYMENT_METHOD = listOf("payment_method")
        const val MAX_POLLING_DURATION = 15000L
        const val REDUCED_POLLING_DURATION = 5000L
        const val POLLING_DELAY = 1000L
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
