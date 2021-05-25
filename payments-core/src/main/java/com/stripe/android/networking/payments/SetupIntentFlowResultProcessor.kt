package com.stripe.android.payments

import android.content.Context
import com.stripe.android.Logger
import com.stripe.android.SetupIntentResult
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Processes the result of a [SetupIntent] confirmation.
 */
internal class SetupIntentFlowResultProcessor(
    context: Context,
    private val publishableKey: String,
    private val stripeRepository: StripeRepository,
    enableLogging: Boolean,
    private val workContext: CoroutineContext
) : PaymentFlowResultProcessor<SetupIntentResult> {
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
            stripeRepository.retrieveSetupIntent(
                result.clientSecret,
                requestOptions,
                expandFields = PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD
            )
        ).let { setupIntent ->
            if (shouldCancelIntent(setupIntent, result.canCancelSource)) {
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
}
