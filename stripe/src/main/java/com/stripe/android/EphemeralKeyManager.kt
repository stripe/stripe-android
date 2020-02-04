package com.stripe.android

import com.stripe.android.model.parsers.EphemeralKeyJsonParser
import java.net.HttpURLConnection
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.json.JSONException
import org.json.JSONObject

internal class EphemeralKeyManager(
    private val ephemeralKeyProvider: EphemeralKeyProvider,
    private val listener: KeyManagerListener,
    private val timeBufferInSeconds: Long,
    private val overrideCalendar: Calendar?,
    operationIdFactory: OperationIdFactory,
    shouldPrefetchEphemeralKey: Boolean
) {
    private val apiVersion: String = ApiVersion.get().code

    private var ephemeralKey: EphemeralKey? = null

    init {
        if (shouldPrefetchEphemeralKey) {
            retrieveEphemeralKey(
                EphemeralOperation.RetrieveKey(operationIdFactory.create())
            )
        }
    }

    @JvmSynthetic
    internal fun retrieveEphemeralKey(
        operation: EphemeralOperation
    ) {
        ephemeralKey.takeUnless {
            it == null || shouldRefreshKey(it, timeBufferInSeconds, overrideCalendar)
        }?.let { ephemeralKey ->
            listener.onKeyUpdate(ephemeralKey, operation)
        } ?: ephemeralKeyProvider.createEphemeralKey(
            apiVersion,
            ClientKeyUpdateListener(this, operation)
        )
    }

    private fun updateKey(
        operation: EphemeralOperation,
        key: String?
    ) {
        // Key is coming from the user, so even if it's @NonNull annotated we
        // want to double check it
        if (key == null) {
            listener.onKeyError(
                operation.id,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "EphemeralKeyUpdateListener.onKeyUpdate was called with a null value"
            )
            return
        }
        try {
            val ephemeralKey = EphemeralKeyJsonParser().parse(JSONObject(key))
            this.ephemeralKey = ephemeralKey
            listener.onKeyUpdate(ephemeralKey, operation)
        } catch (e: JSONException) {
            listener.onKeyError(
                operation.id,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                    "a value that could not be JSON parsed: [${e.localizedMessage}]. " +
                    "The raw body from Stripe's response should be passed."
            )
        } catch (e: Exception) {
            listener.onKeyError(
                operation.id,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                    "a JSON String that was invalid: [${e.localizedMessage}]. " +
                    "The raw body from Stripe's response should be passed."
            )
        }
    }

    private fun updateKeyError(operationId: String, errorCode: Int, errorMessage: String) {
        ephemeralKey = null
        listener.onKeyError(operationId, errorCode, errorMessage)
    }

    internal interface KeyManagerListener {
        fun onKeyUpdate(
            ephemeralKey: EphemeralKey,
            operation: EphemeralOperation
        )

        fun onKeyError(
            operationId: String,
            errorCode: Int,
            errorMessage: String
        )
    }

    private class ClientKeyUpdateListener internal constructor(
        private val ephemeralKeyManager: EphemeralKeyManager,
        private val operation: EphemeralOperation
    ) : EphemeralKeyUpdateListener {
        override fun onKeyUpdate(stripeResponseJson: String) {
            ephemeralKeyManager.updateKey(operation, stripeResponseJson)
        }

        override fun onKeyUpdateFailure(responseCode: Int, message: String) {
            ephemeralKeyManager.updateKeyError(operation.id, responseCode, message)
        }
    }

    internal companion object {
        internal fun shouldRefreshKey(
            ephemeralKey: EphemeralKey?,
            bufferInSeconds: Long,
            proxyCalendar: Calendar?
        ): Boolean {
            if (ephemeralKey == null) {
                return true
            }

            val now = proxyCalendar ?: Calendar.getInstance()
            val nowInSeconds = TimeUnit.MILLISECONDS.toSeconds(now.timeInMillis)
            val nowPlusBuffer = nowInSeconds + bufferInSeconds
            return ephemeralKey.expires < nowPlusBuffer
        }
    }
}
