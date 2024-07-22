package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import javax.inject.Inject

internal const val KeyConsumerPublishableKey = "ConsumerPublishableKey"
private const val KeyTentativeConsumerPublishableKey = "KeyTentativeConsumerPublishableKey"

internal interface ApiRequestOptionsProvider {
    fun provideApiRequestOptions(): ApiRequest.Options
    suspend fun setTentativeConsumerPublishableKey(key: String?)
    suspend fun confirmConsumerPublishableKey()
}

internal class RealApiRequestOptionsProvider @Inject constructor(
    private val originalRequestOptions: ApiRequest.Options,
    private val savedStateHandle: SavedStateHandle,
    private val isLinkWithStripe: IsLinkWithStripe,
) : ApiRequestOptionsProvider {

    override fun provideApiRequestOptions(): ApiRequest.Options {
        val consumerKey = retrieveConsumerPublishableKey()

        return if (consumerKey != null) {
            ApiRequest.Options(apiKey = consumerKey)
        } else {
            originalRequestOptions
        }
    }

    override suspend fun setTentativeConsumerPublishableKey(key: String?) {
        if (isLinkWithStripe()) {
            savedStateHandle[KeyTentativeConsumerPublishableKey] = key
        }
    }

    override suspend fun confirmConsumerPublishableKey() {
        if (isLinkWithStripe()) {
            val key = retrieveTentativeConsumerPublishableKey()
            savedStateHandle[KeyConsumerPublishableKey] = key
        }
    }

    private fun retrieveConsumerPublishableKey(): String? {
        return savedStateHandle[KeyConsumerPublishableKey]
    }

    private fun retrieveTentativeConsumerPublishableKey(): String? {
        return savedStateHandle[KeyTentativeConsumerPublishableKey]
    }
}
