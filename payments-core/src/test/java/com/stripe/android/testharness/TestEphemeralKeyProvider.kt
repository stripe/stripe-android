package com.stripe.android.testharness

import androidx.annotation.Size
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import org.json.JSONObject

/**
 * An [EphemeralKeyProvider] to be used in tests that automatically returns test values.
 */
internal class TestEphemeralKeyProvider : EphemeralKeyProvider {
    private var errorCode = INVALID_ERROR_CODE
    private var errorMessage: String? = null
    private var rawEphemeralKey: String? = null

    override fun createEphemeralKey(
        @Size(min = 4) apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {
        val rawEphemeralKey = this.rawEphemeralKey
        when {
            rawEphemeralKey != null ->
                keyUpdateListener.onKeyUpdate(rawEphemeralKey)
            errorCode != INVALID_ERROR_CODE ->
                keyUpdateListener.onKeyUpdateFailure(errorCode, errorMessage.orEmpty())
            else -> // Useful to test edge cases
                keyUpdateListener.onKeyUpdate("")
        }
    }

    fun setNextRawEphemeralKey(ephemeralKey: JSONObject) {
        setNextRawEphemeralKey(ephemeralKey.toString())
    }

    fun setNextRawEphemeralKey(rawEphemeralKey: String) {
        this.rawEphemeralKey = rawEphemeralKey
        errorCode = INVALID_ERROR_CODE
        errorMessage = ""
    }

    fun setNextError(errorCode: Int, errorMessage: String) {
        rawEphemeralKey = null
        this.errorCode = errorCode
        this.errorMessage = errorMessage
    }

    private companion object {
        private const val INVALID_ERROR_CODE = -1
    }
}
