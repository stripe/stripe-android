package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException

internal interface ApiRequestExecutor {
    @Throws(APIConnectionException::class, InvalidRequestException::class)
    fun execute(request: ApiRequest): StripeResponse
}
