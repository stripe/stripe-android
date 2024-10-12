package com.stripe.android.core.error

import androidx.annotation.RestrictTo
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.responseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SentryRequestExecutor @Inject constructor(
    private val networkClient: StripeNetworkClient,
    private val logger: Logger
) {

    suspend fun sendErrorRequest(request: SentryEnvelopeRequest) {
        withContext(Dispatchers.IO) {
            runCatching {
                logger.debug("Sentry request: ${request.envelopeBody}")
                logger.debug("Sentry headers: ${request.headers}")
                networkClient.executeRequest(request).also {
                    logger.debug("Sentry response: ${it.responseJson()}")
                }
            }.onFailure {
                // TODO enqueue using WM.
                logger.error("Sentry Exception while sending error report", it)
            }
        }
    }
}
