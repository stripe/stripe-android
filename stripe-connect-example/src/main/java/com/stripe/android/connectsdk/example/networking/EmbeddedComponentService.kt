package com.stripe.android.connectsdk.example.networking

import kotlinx.coroutines.delay

class EmbeddedComponentService {
    suspend fun fetchClientSecret(): String? {
        // TODO MXMOBILE-2511 - add backend call
        delay(3000)
        return null
    }
}
