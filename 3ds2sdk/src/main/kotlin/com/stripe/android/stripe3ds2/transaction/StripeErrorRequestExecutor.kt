package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.transactions.ErrorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * A class responsible for posting error messages to the ACS.
 */
internal class StripeErrorRequestExecutor(
    private val httpClient: HttpClient,
    private val errorReporter: ErrorReporter,
    private val workContext: CoroutineContext
) : ErrorRequestExecutor {
    override fun executeAsync(
        errorData: ErrorData
    ) {
        // there's no further action if JSON conversion fails
        runCatching {
            errorData.toJson().toString()
        }.onFailure {
            errorReporter.reportError(
                RuntimeException("Could not convert ErrorData to JSON.\n$$errorData", it)
            )
        }.getOrNull()?.let { requestBody ->
            CoroutineScope(workContext).launch {
                // ignore exception at this point because this should be treated as a
                // "fire and forget" request
                runCatching {
                    httpClient.doPostRequest(requestBody, CONTENT_TYPE)
                }.onFailure {
                    errorReporter.reportError(it)
                }
            }
        }
    }

    internal class Factory(
        private val workContext: CoroutineContext
    ) : ErrorRequestExecutor.Factory {
        override fun create(
            acsUrl: String,
            errorReporter: ErrorReporter
        ): ErrorRequestExecutor {
            return StripeErrorRequestExecutor(
                StripeHttpClient(
                    acsUrl,
                    errorReporter = errorReporter,
                    workContext = workContext
                ),
                errorReporter,
                Dispatchers.IO
            )
        }
    }

    private companion object {
        private const val CONTENT_TYPE = "application/json; charset=utf-8"
    }
}
