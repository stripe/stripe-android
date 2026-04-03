package com.stripe.android.core

import android.util.Log

internal actual fun createPlatformLogger(): Logger = AndroidLogger

private object AndroidLogger : Logger {
    private const val TAG = "StripeSdk"

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
