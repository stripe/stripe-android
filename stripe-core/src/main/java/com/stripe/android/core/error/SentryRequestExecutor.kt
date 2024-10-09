package com.stripe.android.core.error

import androidx.annotation.RestrictTo
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.StripeNetworkClient
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
                networkClient.executeRequest(request)

                logger.debug("Sent Sentry error report")
            }.onFailure {
                // TODO enqueue using WM.
                logger.error("Exception while sending Sentry error report", it)
            }
        }
    }
}
