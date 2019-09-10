package com.stripe.android.utils

object ObjectUtils {
    @JvmStatic
    fun <T> getOrDefault(obj: T?, defaultValue: T): T {
        return obj ?: defaultValue
    }

    @JvmStatic
    fun <T : Collection<*>> getOrEmpty(obj: T?, emptyValue: T): T {
        return if (obj != null && !obj.isEmpty()) { obj } else { emptyValue }
    }
}
