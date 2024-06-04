package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
enum class VerificationType(val value: String) {
    EMAIL("EMAIL"), SMS("SMS")
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
enum class CustomEmailType(val value: String) {
    LINK_OTP_EMAIL("LINK_OTP_EMAIL"),
    NETWORKED_CONNECTIONS_OTP_EMAIL("NETWORKED_CONNECTIONS_OTP_EMAIL"),
}
