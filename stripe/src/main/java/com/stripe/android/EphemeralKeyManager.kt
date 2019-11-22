package com.stripe.android

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
            retrieveEphemeralKey(operationIdFactory.create(), null, null)
        }
    }

    fun retrieveEphemeralKey(
        operationId: String,
        actionString: String?,
        arguments: Map<String, Any>?
    ) {
        val ephemeralKey = ephemeralKey
        if (ephemeralKey == null ||
            shouldRefreshKey(ephemeralKey, timeBufferInSeconds, overrideCalendar)) {
            ephemeralKeyProvider.createEphemeralKey(apiVersion,
                ClientKeyUpdateListener(this, operationId, actionString, arguments))
        } else {
            listener.onKeyUpdate(ephemeralKey, operationId, actionString, arguments)
        }
    }

    private fun updateKey(
        operationId: String,
        key: String?,
        actionString: String?,
        arguments: Map<String, Any>?
    ) {
        // Key is coming from the user, so even if it's @NonNull annotated we
        // want to double check it
        if (key == null) {
            listener.onKeyError(operationId,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "EphemeralKeyUpdateListener.onKeyUpdate was called with a null value")
            return
        }
        try {
            val ephemeralKey = EphemeralKey.fromJson(JSONObject(key))
            this.ephemeralKey = ephemeralKey
            listener.onKeyUpdate(ephemeralKey, operationId, actionString, arguments)
        } catch (e: JSONException) {
            listener.onKeyError(operationId,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                    "a value that could not be JSON parsed: [${e.localizedMessage}]. " +
                    "The raw body from Stripe's response should be passed.")
        } catch (e: Exception) {
            listener.onKeyError(operationId,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                    "a JSON String that was invalid: [${e.localizedMessage}]. " +
                    "The raw body from Stripe's response should be passed.")
        }
    }

    private fun updateKeyError(operationId: String, errorCode: Int, errorMessage: String) {
        ephemeralKey = null
        listener.onKeyError(operationId, errorCode, errorMessage)
    }

    internal interface KeyManagerListener {
        fun onKeyUpdate(
            ephemeralKey: EphemeralKey,
            operationId: String,
            action: String?,
            arguments: Map<String, Any>?
        )

        fun onKeyError(
            operationId: String,
            errorCode: Int,
            errorMessage: String
        )
    }

    private class ClientKeyUpdateListener internal constructor(
        private val ephemeralKeyManager: EphemeralKeyManager,
        private val operationId: String,
        private val actionString: String?,
        private val arguments: Map<String, Any>?
    ) : EphemeralKeyUpdateListener {
        override fun onKeyUpdate(stripeResponseJson: String) {
            ephemeralKeyManager.updateKey(operationId, stripeResponseJson, actionString, arguments)
        }

        override fun onKeyUpdateFailure(responseCode: Int, message: String) {
            ephemeralKeyManager.updateKeyError(operationId, responseCode, message)
        }
    }

    companion object {
        fun shouldRefreshKey(
            key: EphemeralKey?,
            bufferInSeconds: Long,
            proxyCalendar: Calendar?
        ): Boolean {
            if (key == null) {
                return true
            }

            val now = proxyCalendar ?: Calendar.getInstance()
            val nowInSeconds = TimeUnit.MILLISECONDS.toSeconds(now.timeInMillis)
            val nowPlusBuffer = nowInSeconds + bufferInSeconds
            return key.expires < nowPlusBuffer
        }
    }
}
