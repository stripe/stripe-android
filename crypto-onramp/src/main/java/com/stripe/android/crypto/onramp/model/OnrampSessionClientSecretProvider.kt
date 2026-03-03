package com.stripe.android.crypto.onramp.model

internal fun interface OnrampSessionClientSecretProvider {
    suspend fun getClientSecret(onrampSessionId: String): String
}
