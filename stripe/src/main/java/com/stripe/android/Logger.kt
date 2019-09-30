package com.stripe.android

import android.util.Log

internal interface Logger {
    fun error(msg: String, t: Throwable? = null)

    fun info(msg: String)

    companion object {
        internal fun getInstance(enableLogging: Boolean): Logger {
            return if (enableLogging) {
                real()
            } else {
                noop()
            }
        }

        private const val TAG = "StripeSdk"

        private val REAL_LOGGER = object : Logger {
            override fun info(msg: String) {
                Log.i(TAG, msg)
            }

            override fun error(msg: String, t: Throwable?) {
                Log.e(TAG, msg, t)
            }
        }

        private val NOOP_LOGGER = object : Logger {
            override fun info(msg: String) {
            }

            override fun error(msg: String, t: Throwable?) {
            }
        }

        internal fun real(): Logger {
            return REAL_LOGGER
        }

        internal fun noop(): Logger {
            return NOOP_LOGGER
        }
    }
}
