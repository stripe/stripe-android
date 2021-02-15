package com.stripe.android.payments

import android.content.Context
import com.stripe.android.Logger
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DefaultPaymentFlowResultProcessor(
    context: Context,
    private val publishableKey: String,
    private val stripeRepository: StripeRepository,
    enableLogging: Boolean,
    private val workContext: CoroutineContext
) : PaymentFlowResultProcessor {
    private val logger = Logger.getInstance(enableLogging)
    private val failureMessageFactory = PaymentFlowFailureMessageFactory(context)

    override suspend fun processPaymentIntent(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): PaymentIntentResult = withContext(workContext) {
        val result = unvalidatedResult.validate()

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = result.stripeAccountId
        )

        requireNotNull(
            stripeRepository.retrievePaymentIntent(
                result.clientSecret,
                requestOptions,
                expandFields = EXPAND_PAYMENT_METHOD
            )
        ).let { paymentIntent ->
            if (result.shouldCancelSource && paymentIntent.requiresAction()) {
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

    override suspend fun processSetupIntent(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): SetupIntentResult = withContext(workContext) {
        val result = unvalidatedResult.validate()

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = result.stripeAccountId
        )

        requireNotNull(
            stripeRepository.retrieveSetupIntent(
                result.clientSecret,
                requestOptions,
                expandFields = EXPAND_PAYMENT_METHOD
            )
        ).let { setupIntent ->
            if (result.shouldCancelSource && setupIntent.requiresAction()) {
                cancelSetupIntent(
                    setupIntent,
                    requestOptions,
                    result.sourceId.orEmpty(),
                )
            } else {
                setupIntent
            }
        }.let { setupIntent ->
            SetupIntentResult(
                setupIntent,
                result.flowOutcome,
                failureMessageFactory.create(setupIntent, result.flowOutcome)
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

    private suspend fun cancelSetupIntent(
        setupIntent: SetupIntent,
        requestOptions: ApiRequest.Options,
        sourceId: String
    ): SetupIntent {
        logger.debug("Canceling source '$sourceId' for SetupIntent")

        return requireNotNull(
            stripeRepository.cancelSetupIntentSource(
                setupIntent.id.orEmpty(),
                sourceId,
                requestOptions,
            )
        )
    }

    private companion object {
        private val EXPAND_PAYMENT_METHOD = listOf("payment_method")
    }
}
