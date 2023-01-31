package com.stripe.android.financialconnections.domain

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.repository.ConsumersApiService
import javax.inject.Inject

internal class LookupAccount @Inject constructor(
    private val apiOptions: ApiRequest.Options,
    private val consumersApiService: ConsumersApiService
) {

    suspend operator fun invoke(
        email: String
    ): ConsumerSessionLookup? {
        return consumersApiService.lookupConsumerSession(
            email = email,
            authSessionCookie = null,
            requestOptions = apiOptions
        )
    }
}
