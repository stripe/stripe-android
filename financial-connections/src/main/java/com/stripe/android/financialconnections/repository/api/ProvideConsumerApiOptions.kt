package com.stripe.android.financialconnections.repository.api

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import javax.inject.Inject

internal fun interface ProvideConsumerApiOptions {
    suspend operator fun invoke(): ApiRequest.Options?
}

internal class RealProvideConsumerApiOptions @Inject constructor(
    private val consumerSessionProvider: ConsumerSessionProvider,
    private val isLinkWithStripe: IsLinkWithStripe,
) : ProvideConsumerApiOptions {

    override suspend operator fun invoke(): ApiRequest.Options? {
        val session = consumerSessionProvider.provideConsumerSession()?.takeIf { it.isVerified }
        val consumerPublishableKey = session?.publishableKey?.takeIf { isLinkWithStripe() }

        return consumerPublishableKey?.let {
            ApiRequest.Options(apiKey = it)
        }
    }
}
