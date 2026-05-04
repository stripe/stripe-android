package com.stripe.android.lpm

internal interface MpeLatencyReporter {
    fun onStart(testName: String)

    fun onLoad(testName: String)
}
