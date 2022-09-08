package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class CvcCheck(
    val code: String
) {
    Pass("PASS"),
    Fail("FAIL"),
    Unavailable("UNAVAILABLE"),
    Unchecked("UNCHECKED"),
    Unknown("UNKNOWN");

    val requiresRecollection: Boolean
        get() = this in setOf(Fail, Unavailable, Unchecked)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun fromCode(code: String?): CvcCheck {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Unknown
        }
    }
}
