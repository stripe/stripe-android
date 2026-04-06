package com.stripe.android.core.networking

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

internal actual object PlatformHttpClientFactory : HttpClientFactory {
    override fun create(
        configure: HttpClientConfig<*>.() -> Unit
    ): HttpClient {
        return HttpClient(OkHttp) {
            configure()
        }
    }
}
