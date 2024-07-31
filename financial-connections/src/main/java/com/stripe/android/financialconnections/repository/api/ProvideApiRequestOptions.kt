package com.stripe.android.financialconnections.repository.api

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import javax.inject.Inject

internal fun interface ProvideApiRequestOptions {
    operator fun invoke(useConsumerPublishableKey: Boolean): ApiRequest.Options
}

internal class RealProvideApiRequestOptions @Inject constructor(
    private val consumerSessionProvider: ConsumerSessionProvider,
    private val isLinkWithStripe: IsLinkWithStripe,
    private val apiRequestOptions: ApiRequest.Options,
) : ProvideApiRequestOptions {

    override fun invoke(
        useConsumerPublishableKey: Boolean,
    ): ApiRequest.Options {
        return if (useConsumerPublishableKey) {
            consumerApiRequestOptions() ?: apiRequestOptions
        } else {
            apiRequestOptions
        }
    }

    private fun consumerApiRequestOptions(): ApiRequest.Options? {
        val session = consumerSessionProvider.provideConsumerSession()?.takeIf { it.isVerified }
        val consumerPublishableKey = session?.publishableKey?.takeIf { isLinkWithStripe() }

        return consumerPublishableKey?.let {
            ApiRequest.Options(apiKey = it)
        }
    }
}
