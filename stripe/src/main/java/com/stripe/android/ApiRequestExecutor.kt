package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import java.net.UnknownHostException

internal interface ApiRequestExecutor {
    @Throws(APIConnectionException::class, InvalidRequestException::class, UnknownHostException::class)
    fun execute(request: ApiRequest): StripeResponse
}
