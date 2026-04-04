package com.stripe.android.core.exception

internal expect fun isJsonException(throwable: Throwable): Boolean

internal expect fun throwableClassName(throwable: Throwable): String?
