package com.stripe.android.payments.core.authentication

import com.stripe.android.core.injection.IOContext
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.CoroutineContext

private const val RedirectTimeoutInMillis = 10_000

internal fun interface RedirectResolver {
    suspend operator fun invoke(url: String): String
}

internal typealias ConfigureSslHandler = HttpsURLConnection.() -> Unit

internal class RealRedirectResolver(
    private val configureSSL: ConfigureSslHandler,
    private val ioDispatcher: CoroutineContext,
) : RedirectResolver {

    @Inject
    constructor(
        @IOContext ioDispatcher: CoroutineContext
    ) : this(configureSSL = {}, ioDispatcher = ioDispatcher)

    override suspend fun invoke(url: String): String {
        return withContext(ioDispatcher) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = RedirectTimeoutInMillis
                    readTimeout = RedirectTimeoutInMillis
                    instanceFollowRedirects = false

                    if (this is HttpsURLConnection) {
                        configureSSL()
                    }
                }

                val locationHeader = connection.getHeaderField("Location")
                if (!locationHeader.isNullOrEmpty()) {
                    locationHeader
                } else {
                    url
                }
            }.getOrElse {
                url
            }
        }
    }
}
