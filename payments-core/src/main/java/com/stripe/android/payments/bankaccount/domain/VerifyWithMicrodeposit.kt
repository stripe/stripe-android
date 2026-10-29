package com.stripe.android.payments.bankaccount.domain

import com.stripe.android.core.networking.RequestCredentials
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
        secondAmount: Int
    ): Result<PaymentIntent> = stripeRepository.verifyPaymentIntentWithMicrodeposits(
        clientSecret = clientSecret,
        firstAmount = firstAmount,
        secondAmount = secondAmount,
        requestOptions = RequestCredentials(publishableKey, stripeAccount = null),
        idempotencyKey = null,
    )

    /**
     * Verifies a PaymentIntent given a microdeposit,
     * using [descriptorCode] and the intent [clientSecret].
     *
     * @return [PaymentIntent].
     */
    suspend fun forPaymentIntent(
        publishableKey: String,
        clientSecret: String,
        descriptorCode: String
    ): Result<PaymentIntent> = stripeRepository.verifyPaymentIntentWithMicrodeposits(
        clientSecret = clientSecret,
        descriptorCode = descriptorCode,
        requestOptions = RequestCredentials(publishableKey, stripeAccount = null),
        idempotencyKey = null,
    )

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
        secondAmount: Int
    ): Result<SetupIntent> = stripeRepository.verifySetupIntentWithMicrodeposits(
        clientSecret = clientSecret,
        firstAmount = firstAmount,
        secondAmount = secondAmount,
        requestOptions = RequestCredentials(publishableKey, stripeAccount = null),
        idempotencyKey = null,
    )

    /**
     * Verifies a PaymentIntent given a microdeposit,
     * using [descriptorCode] and the intent [clientSecret].
     *
     * @return [PaymentIntent].
     */
    suspend fun forSetupIntent(
        publishableKey: String,
        clientSecret: String,
        descriptorCode: String
    ): Result<SetupIntent> = stripeRepository.verifySetupIntentWithMicrodeposits(
        clientSecret = clientSecret,
        descriptorCode = descriptorCode,
        requestOptions = RequestCredentials(publishableKey, stripeAccount = null),
        idempotencyKey = null,
    )
}
