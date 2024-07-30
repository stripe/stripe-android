package com.stripe.android.stripe3ds2.transaction

import android.util.Log

internal sealed class Logger {
    abstract fun error(msg: String, t: Throwable? = null)
    abstract fun info(msg: String)

    object Real : Logger() {
        override fun info(msg: String) {
            Log.i(TAG, msg)
        }

        override fun error(msg: String, t: Throwable?) {
            Log.e(TAG, msg, t)
        }

        private const val TAG = "StripeSdk"
    }

    object Noop : Logger() {
        override fun error(msg: String, t: Throwable?) {
        }

        override fun info(msg: String) {
        }
    }

    internal companion object {
        fun get(enableLogging: Boolean): Logger {
            return if (enableLogging) {
                Real
            } else {
                Noop
            }
        }
    }
}
