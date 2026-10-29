package com.stripe.android.financialconnections.repository.api

import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import javax.inject.Inject

internal fun interface ProvideApiRequestOptions {
    operator fun invoke(useConsumerPublishableKey: Boolean): RequestCredentials
}

internal class RealProvideApiRequestOptions @Inject constructor(
    private val consumerSessionProvider: ConsumerSessionProvider,
    private val isLinkWithStripe: IsLinkWithStripe,
    private val apiRequestOptions: RequestCredentials,
) : ProvideApiRequestOptions {

    override fun invoke(
        useConsumerPublishableKey: Boolean,
    ): RequestCredentials {
        return if (useConsumerPublishableKey) {
            consumerApiRequestOptions() ?: apiRequestOptions
        } else {
            apiRequestOptions
        }
    }

    private fun consumerApiRequestOptions(): RequestCredentials? {
        val session = consumerSessionProvider.provideConsumerSession()?.takeIf { it.isVerified }
        val consumerPublishableKey = session?.publishableKey?.takeIf { isLinkWithStripe() }

        return consumerPublishableKey?.let {
            RequestCredentials(apiKey = it, stripeAccount = null)
        }
    }
}
