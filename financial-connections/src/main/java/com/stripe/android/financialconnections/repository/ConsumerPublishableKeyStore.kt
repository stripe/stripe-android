package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import javax.inject.Inject

internal const val KeyConsumerPublishableKey = "ConsumerPublishableKey"
private const val KeyTentativeConsumerPublishableKey = "KeyTentativeConsumerPublishableKey"

internal interface ConsumerPublishableKeyStore {
    suspend fun setTentativeConsumerPublishableKey(key: String?)
    suspend fun confirmConsumerPublishableKey()
}

internal fun interface ConsumerPublishableKeyProvider {
    fun provideConsumerPublishableKey(): String?
}

internal class ConsumerPublishableKeyManager @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val isLinkWithStripe: IsLinkWithStripe,
) : ConsumerPublishableKeyStore, ConsumerPublishableKeyProvider {

    override fun provideConsumerPublishableKey(): String? {
        return savedStateHandle[KeyConsumerPublishableKey]
    }

    override suspend fun setTentativeConsumerPublishableKey(key: String?) {
        if (isLinkWithStripe()) {
            savedStateHandle[KeyTentativeConsumerPublishableKey] = key
        }
    }

    override suspend fun confirmConsumerPublishableKey() {
        if (isLinkWithStripe()) {
            val key = savedStateHandle.get<String>(KeyTentativeConsumerPublishableKey)
            savedStateHandle[KeyConsumerPublishableKey] = key
        }
    }
}

/**
 * Returns [ApiRequest.Options] with the previously received consumer publishable key (stored via
 * [ConsumerPublishableKeyStore]) or null if none was received.
 */
internal fun ConsumerPublishableKeyProvider.createApiRequestOptions(): ApiRequest.Options? {
    return provideConsumerPublishableKey()?.let { ApiRequest.Options(apiKey = it) }
}
