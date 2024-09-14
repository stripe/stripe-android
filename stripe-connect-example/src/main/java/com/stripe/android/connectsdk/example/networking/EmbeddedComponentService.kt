package com.stripe.android.connectsdk.example.networking

import kotlinx.coroutines.delay

class EmbeddedComponentService {
    suspend fun fetchClientSecret(): String? {
        delay(3000)
        return null
    }
}