package com.stripe.android.core.exception

import org.json.JSONException

internal actual fun isJsonException(throwable: Throwable): Boolean {
    return throwable is JSONException
}

internal actual fun throwableClassName(throwable: Throwable): String? {
    return throwable.javaClass.name
}
