package com.stripe.android.connect.util

import com.stripe.android.connect.BuildConfig

/**
 * Returns true if the application is running in an instrumentation test.
 */
internal fun isInInstrumentationTest(): Boolean {
    return BuildConfig.DEBUG &&
        runCatching { Class.forName("com.stripe.android.connect.ConnectTestRunner") }.isSuccess
}
