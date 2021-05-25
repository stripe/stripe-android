package com.stripe.android.payments

import android.content.Context
import com.stripe.android.Logger
import com.stripe.android.PaymentIntentResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Processes the result of a [PaymentIntent] confirmation.
 */
internal class PaymentIntentFlowResultProcessor(
    context: Context,
    private val publishableKey: String,
    private val stripeRepository: StripeRepository,
    enableLogging: Boolean,
    private val workContext: CoroutineContext
) : PaymentFlowResultProcessor<PaymentIntentResult> {
    private val logger = Logger.getInstance(enableLogging)
    private val failureMessageFactory = PaymentFlowFailureMessageFactory(context)

    override suspend fun processResult(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ) = withContext(workContext) {
        val result = unvalidatedResult.validate()

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = result.stripeAccountId
        )

        requireNotNull(
            stripeRepository.retrievePaymentIntent(
                result.clientSecret,
                requestOptions,
                expandFields = PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD
            )
        ).let { paymentIntent ->
            if (shouldCancelIntent(paymentIntent, result.canCancelSource)) {
                cancelPaymentIntent(
                    paymentIntent,
                    requestOptions,
                    result.sourceId.orEmpty(),
                )
            } else {
                paymentIntent
            }
        }.let { paymentIntent ->
            PaymentIntentResult(
                paymentIntent,
                result.flowOutcome,
                failureMessageFactory.create(paymentIntent, result.flowOutcome)
            )
        }
    }

    private suspend fun cancelPaymentIntent(
        paymentIntent: PaymentIntent,
        requestOptions: ApiRequest.Options,
        sourceId: String
    ): PaymentIntent {
        logger.debug("Canceling source '$sourceId' for PaymentIntent")

        return requireNotNull(
            stripeRepository.cancelPaymentIntentSource(
                paymentIntent.id.orEmpty(),
                sourceId,
                requestOptions,
            )
        )
    }
}
