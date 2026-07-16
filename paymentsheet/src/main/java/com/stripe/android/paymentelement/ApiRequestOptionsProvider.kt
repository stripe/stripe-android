package com.stripe.android.paymentelement

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal fun interface ApiRequestOptionsProvider {
    fun get(): ApiRequest.Options
}

@Singleton
internal class MutableApiRequestOptionsProvider @Inject constructor(
    private val paymentConfig: Provider<PaymentConfiguration>,
) : ApiRequestOptionsProvider {
    @Volatile
    private var apiConfiguration: ApiConfiguration? = null

    fun update(config: ApiConfiguration?) {
        apiConfiguration = config
    }

    override fun get(): ApiRequest.Options {
        return apiConfiguration?.let {
            ApiRequest.Options(apiKey = it.publishableKey, stripeAccount = it.stripeAccountId)
        } ?: ApiRequest.Options(
            apiKey = paymentConfig.get().publishableKey,
            stripeAccount = paymentConfig.get().stripeAccountId,
        )
    }
}

// TODO: Replace with the real ApiConfiguration once it is available.
internal data class ApiConfiguration(
    val publishableKey: String,
    val stripeAccountId: String? = null,
)
