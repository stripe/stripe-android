package com.stripe.android.payments.bankaccount.domain

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
        publishableKey: String,
        clientSecret: String
    ): Result<StripeIntent> {
        return stripeRepository.retrieveStripeIntent(
            clientSecret = clientSecret,
            options = ApiRequest.Options(publishableKey)
        )
    }
}
