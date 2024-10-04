package com.stripe.android.connectsdk.example.networking

import kotlinx.coroutines.delay

class EmbeddedComponentService {
    suspend fun fetchClientSecret(): String? {
        // TODO MXMOBILE-2511 - add backend call
        delay(ARTIFICIAL_DELAY_MS)
        return null
    }

    companion object {
        private const val ARTIFICIAL_DELAY_MS = 3000L
    }
}
