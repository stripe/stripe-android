package com.stripe.android.payments.bankaccount.domain

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal class RetrieveStripeIntent @Inject constructor(
    private val stripeRepository: StripeRepository
) {

    /**
     * Retrieve [StripeIntent].
     */
    suspend operator fun invoke(
        apiConfiguration: ApiConfiguration.State,
        clientSecret: String,
    ): Result<StripeIntent> {
        return stripeRepository.retrieveStripeIntent(
            clientSecret = clientSecret,
            options = ApiRequest.Options(
                apiKey = apiConfiguration.publishableKey,
                stripeAccount = apiConfiguration.stripeAccountId,
            ),
        )
    }
}
