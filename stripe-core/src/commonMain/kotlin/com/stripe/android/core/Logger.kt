package com.stripe.android.core

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Logger {
    fun debug(msg: String)

    fun info(msg: String)

    fun warning(msg: String)

    fun error(msg: String, t: Throwable? = null)

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getInstance(enableLogging: Boolean): Logger =
            if (enableLogging) {
                real()
            } else {
                noop()
            }

        fun real(): Logger = createPlatformLogger()

        fun noop(): Logger = NoopLogger
    }
}

internal expect fun createPlatformLogger(): Logger

private object NoopLogger : Logger {
    override fun debug(msg: String) = Unit

    override fun info(msg: String) = Unit

    override fun warning(msg: String) = Unit

    override fun error(msg: String, t: Throwable?) = Unit
}
