package com.stripe.android.payments

import android.content.Context
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository

/**
 * Cancels a 3DS source before the authentication activity returns its result to its caller.
 *
 * The caller may be destroyed before its parent processes the result, so this operation must not
 * depend on [PaymentFlowResultProcessor].
 */
internal class SourceCancellationHandler(
    private val args: PaymentBrowserAuthContract.Args,
    private val stripeRepository: StripeRepository,
    private val logger: Logger,
) {
    suspend fun cancel(): Boolean {
        if (!args.shouldCancelSource || args.sourceId == null) {
            return false
        }

        logger.debug("SourceCancellationHandler#cancel()")

        val requestOptions = ApiRequest.Options(
            apiKey = args.publishableKey,
            stripeAccount = args.stripeAccountId,
        )

        return when {
            args.clientSecret.startsWith("pi_") -> {
                val paymentIntent = stripeRepository.retrievePaymentIntent(args.clientSecret, requestOptions)
                    .getOrElse { return logFailure(it) }
                cancelPaymentIntentSource(paymentIntent)
            }
            args.clientSecret.startsWith("seti_") -> {
                val setupIntent = stripeRepository.retrieveSetupIntent(args.clientSecret, requestOptions)
                    .getOrElse { return logFailure(it) }
                cancelSetupIntentSource(setupIntent)
            }
            else -> false
        }
    }

    private suspend fun cancelPaymentIntentSource(paymentIntent: PaymentIntent): Boolean {
        if (!paymentIntent.requiresAction()) {
            logger.debug("SourceCancellationHandler#cancel() - PaymentIntent no longer requires action")
            return true
        }

        val (intentId, requestOptions, sourceId) = cancellationParameters(paymentIntent)
        val result = stripeRepository.cancelPaymentIntentSource(intentId, sourceId, requestOptions)
            .exceptionOrNull()
            ?.let(::logFailure)
            ?: true
        if (result) {
            logger.debug("SourceCancellationHandler#cancel() - PaymentIntent source canceled")
        }
        return result
    }

    private suspend fun cancelSetupIntentSource(setupIntent: SetupIntent): Boolean {
        if (!setupIntent.requiresAction()) {
            logger.debug("SourceCancellationHandler#cancel() - SetupIntent no longer requires action")
            return true
        }

        val (intentId, requestOptions, sourceId) = cancellationParameters(setupIntent)
        val result = stripeRepository.cancelSetupIntentSource(intentId, sourceId, requestOptions)
            .exceptionOrNull()
            ?.let(::logFailure)
            ?: true
        if (result) {
            logger.debug("SourceCancellationHandler#cancel() - SetupIntent source canceled")
        }
        return result
    }

    private fun cancellationParameters(stripeIntent: StripeIntent): CancellationParameters {
        val threeDS2Data = stripeIntent.nextActionData as? StripeIntent.NextActionData.SdkData.Use3DS2
        return CancellationParameters(
            intentId = threeDS2Data?.threeDS2IntentId ?: stripeIntent.id.orEmpty(),
            requestOptions = threeDS2Data?.publishableKey?.let { ApiRequest.Options(it) }
                ?: ApiRequest.Options(args.publishableKey, args.stripeAccountId),
            sourceId = threeDS2Data?.source ?: requireNotNull(args.sourceId),
        )
    }

    private fun logFailure(error: Throwable): Boolean {
        logger.error("Failed to cancel 3DS source.", error)
        return false
    }

    private data class CancellationParameters(
        val intentId: String,
        val requestOptions: ApiRequest.Options,
        val sourceId: String,
    )

    companion object {
        fun create(
            context: Context,
            args: PaymentBrowserAuthContract.Args,
            logger: Logger,
        ): SourceCancellationHandler {
            return SourceCancellationHandler(
                args = args,
                stripeRepository = StripeApiRepository(
                    context = context,
                    publishableKeyProvider = { args.publishableKey },
                    requestSurface = StripeRepository.DEFAULT_REQUEST_SURFACE,
                ),
                logger = logger,
            )
        }
    }
}
