package com.stripe.android.core

import android.util.Log
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Logger {
    fun debug(msg: String)

    fun info(msg: String)

    fun warning(msg: String)

    fun error(msg: String, t: Throwable? = null)

    companion object {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getInstance(enableLogging: Boolean): Logger {
            return if (enableLogging) {
                real()
            } else {
                noop()
            }
        }

        private const val TAG = "StripeSdk"

        private val REAL_LOGGER = object : Logger {
            override fun debug(msg: String) {
                Log.d(TAG, msg)
            }

            override fun info(msg: String) {
                Log.i(TAG, msg)
            }

            override fun warning(msg: String) {
                Log.w(TAG, msg)
            }

            override fun error(msg: String, t: Throwable?) {
                Log.e(TAG, msg, t)
            }
        }

        private val NOOP_LOGGER = object : Logger {
            override fun debug(msg: String) {
            }

            override fun info(msg: String) {
            }

            override fun warning(msg: String) {
            }

            override fun error(msg: String, t: Throwable?) {
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
