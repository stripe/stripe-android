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
    StateInvalid("STATE_INVALID"),
    Unknown("UNKNOWN");

    val requiresRecollection: Boolean
        get() = this in setOf(Fail, Unavailable, Unchecked, StateInvalid)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun fromCode(code: String?): CvcCheck {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Unknown
        }
    }
}
