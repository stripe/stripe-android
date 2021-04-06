package com.stripe.android.networking

internal interface ApiRequestExecutor {
    suspend fun execute(request: ApiRequest): StripeResponse

    suspend fun execute(request: FileUploadRequest): StripeResponse
}
