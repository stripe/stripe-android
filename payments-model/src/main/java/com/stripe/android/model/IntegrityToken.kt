package com.stripe.android.model

data class IntegrityToken(
    val token: String,
    val applicationId: String
) {
    fun toParamMap(): Map<String, Any> {
        return mapOf(
            "android_verification_token" to token,
            "app_id" to applicationId
        )
    }
}