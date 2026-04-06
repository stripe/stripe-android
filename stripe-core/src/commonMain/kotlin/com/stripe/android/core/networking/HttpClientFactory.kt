package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface HttpClientFactory {
    fun create(
        configure: HttpClientConfig<*>.() -> Unit = {}
    ): HttpClient
}

internal expect object PlatformHttpClientFactory : HttpClientFactory
