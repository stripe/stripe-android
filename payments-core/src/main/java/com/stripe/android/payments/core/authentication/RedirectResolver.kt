package com.stripe.android.payments.core.authentication

import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

private const val RedirectTimeoutInMillis = 10_000

internal fun interface RedirectResolver {
    suspend operator fun invoke(url: String): String
}

internal typealias ConfigureSslHandler = HttpsURLConnection.() -> Unit

internal class RealRedirectResolver(
    private val configureSSL: ConfigureSslHandler,
) : RedirectResolver {

    @Inject
    constructor() : this(configureSSL = {})

    override suspend fun invoke(url: String): String {
        return runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = RedirectTimeoutInMillis
                readTimeout = RedirectTimeoutInMillis

                if (this is HttpsURLConnection) {
                    configureSSL()
                }
            }

            // Seems like we need to call getResponseCode() so that HttpURLConnection internally
            // follows the redirect. If we didn't call this method, connection.url would be the
            // same as the provided url, making this method redundant.
            connection.responseCode

            connection.url.toString()
        }.getOrElse {
            url
        }
    }
}
