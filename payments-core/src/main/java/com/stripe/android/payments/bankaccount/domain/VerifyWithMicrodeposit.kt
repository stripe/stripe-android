package com.stripe.android.payments.bankaccount.domain

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal class VerifyWithMicrodeposit @Inject constructor(
    private val stripeRepository: StripeRepository
) {
    /**
     * Verifies a PaymentIntent given a microdeposit,
     * using [firstAmount], [secondAmount] and the intent [clientSecret].
     *
     * @return [PaymentIntent].
     */
    suspend fun forPaymentIntent(
        publishableKey: String,
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
    ): Result<PaymentIntent> = kotlin.runCatching {
        stripeRepository.verifyPaymentIntentWithMicrodeposits(
            clientSecret = clientSecret,
            firstAmount = firstAmount,
            secondAmount = secondAmount,
            requestOptions = ApiRequest.Options(publishableKey)
        )
    }.mapCatching { it ?: throw InternalError("Error verifying PaymentIntent with microdeposits") }

    /**
     * Verifies a PaymentIntent given a microdeposit,
     * using [descriptorCode] and the intent [clientSecret].
     *
     * @return [PaymentIntent].
     */
    suspend fun forPaymentIntent(
        publishableKey: String,
        clientSecret: String,
        descriptorCode: String,
    ): Result<PaymentIntent> = kotlin.runCatching {
        stripeRepository.verifyPaymentIntentWithMicrodeposits(
            clientSecret = clientSecret,
            descriptorCode = descriptorCode,
            requestOptions = ApiRequest.Options(publishableKey)
        )
    }.mapCatching { it ?: throw InternalError("Error verifying PaymentIntent with microdeposits") }

    /**
     * Verifies a SetupIntent given a microdeposit,
     * using [firstAmount], [secondAmount] and the intent [clientSecret].
     *
     * @return [SetupIntent].
     */
    suspend fun forSetupIntent(
        publishableKey: String,
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
    ): Result<SetupIntent> = kotlin.runCatching {
        stripeRepository.verifySetupIntentWithMicrodeposits(
            clientSecret = clientSecret,
            firstAmount = firstAmount,
            secondAmount = secondAmount,
            requestOptions = ApiRequest.Options(publishableKey)
        )
    }.mapCatching { it ?: throw InternalError("Error verifying SetupIntent with microdeposits") }

    /**
     * Verifies a PaymentIntent given a microdeposit,
     * using [descriptorCode] and the intent [clientSecret].
     *
     * @return [PaymentIntent].
     */
    suspend fun forSetupIntent(
        publishableKey: String,
        clientSecret: String,
        descriptorCode: String,
    ): Result<SetupIntent> = kotlin.runCatching {
        stripeRepository.verifySetupIntentWithMicrodeposits(
            clientSecret = clientSecret,
            descriptorCode = descriptorCode,
            requestOptions = ApiRequest.Options(publishableKey)
        )
    }.mapCatching { it ?: throw InternalError("Error verifying SetupIntent with microdeposits") }
}
