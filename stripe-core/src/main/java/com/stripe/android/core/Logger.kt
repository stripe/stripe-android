package com.stripe.android.core

import android.util.Log
import androidx.annotation.RestrictTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Logger {
    val messages: Flow<LogMessage>
    fun debug(msg: String)

    @LogsForInternalTesting
    fun debug(msg: String, tag: String = TAG)

    fun info(msg: String)

    @LogsForInternalTesting
    fun info(msg: String, tag: String = TAG)

    fun warning(msg: String)

    @LogsForInternalTesting
    fun warning(msg: String, tag: String = TAG)

    fun error(msg: String, t: Throwable? = null)

    @LogsForInternalTesting
    fun error(msg: String, t: Throwable? = null, tag: String = TAG)

    fun log(message: LogMessage)

    companion object {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getInstance(enableLogging: Boolean, isExampleApp: Boolean = false): Logger {
            return if (enableLogging || isExampleApp) {
                real()
            } else {
                noop()
            }
        }

        internal const val TAG = "StripeSdk"

        private val REAL_LOGGER = object : Logger {
            override val messages = MutableSharedFlow<LogMessage>()

            override fun debug(msg: String) {
                Log.d(TAG, msg)
            }

            @LogsForInternalTesting
            override fun debug(msg: String, tag: String) {
                log(LogMessage.Debug(msg = msg, tag = tag))
            }

            override fun info(msg: String) {
                Log.i(TAG, msg)
            }

            @LogsForInternalTesting
            override fun info(msg: String, tag: String) {
                log(LogMessage.Info(msg = msg, tag = tag))
            }

            override fun warning(msg: String) {
                Log.w(TAG, msg)
            }

            @LogsForInternalTesting
            override fun warning(msg: String, tag: String) {
                log(LogMessage.Warning(msg = msg, tag = tag))
            }

            override fun error(msg: String, t: Throwable?) {
                Log.e(TAG, msg, t)
            }

            @LogsForInternalTesting
            override fun error(msg: String, t: Throwable?, tag: String) {
                log(LogMessage.Error(msg = msg, throwable = t, tag = tag))
            }

            override fun log(message: LogMessage) {
                when (message) {
                    is LogMessage.Debug -> Log.d(message.tag, message.msg)
                    is LogMessage.Info -> Log.i(message.tag, message.msg)
                    is LogMessage.Warning -> Log.w(message.tag, message.msg)
                    is LogMessage.Error -> Log.e(message.tag, message.msg, message.throwable)
                }
                GlobalScope.launch(Dispatchers.IO) {
                    messages.emit(message)
                }
            }
        }

        private val NOOP_LOGGER = object : Logger {
            override val messages = flowOf<LogMessage>()

            override fun debug(msg: String) {
            }

            @LogsForInternalTesting
            override fun debug(msg: String, tag: String) {
            }

            override fun info(msg: String) {
            }

            @LogsForInternalTesting
            override fun info(msg: String, tag: String) {
            }

            override fun warning(msg: String) {
            }

            @LogsForInternalTesting
            override fun warning(msg: String, tag: String) {
            }

            override fun error(msg: String, t: Throwable?) {
            }

            @LogsForInternalTesting
            override fun error(msg: String, t: Throwable?, tag: String) {
            }

            override fun log(message: LogMessage) {
            }
        }

        fun real(): Logger {
            return REAL_LOGGER
        }

        fun noop(): Logger {
            return NOOP_LOGGER
        }
    }
}

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Only use if you are testing internally (e.g for a bug bash)"
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
annotation class LogsForInternalTesting

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface LogMessage {
    val id: String
    val msg: String
    val tag: String

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Debug(
        override val msg: String,
        override val tag: String = Logger.TAG,
        override val id: String = UUID.randomUUID().toString()
    ) : LogMessage

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Info(
        override val msg: String,
        override val tag: String = Logger.TAG,
        override val id: String = UUID.randomUUID().toString()
    ) : LogMessage

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Warning(
        override val msg: String,
        override val tag: String = Logger.TAG,
        override val id: String = UUID.randomUUID().toString()
    ) : LogMessage

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Error(
        override val msg: String,
        val throwable: Throwable? = null,
        override val tag: String = Logger.TAG,
        override val id: String = UUID.randomUUID().toString()
    ) : LogMessage
}
