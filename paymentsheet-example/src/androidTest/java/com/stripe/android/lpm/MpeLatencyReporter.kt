package com.stripe.android.lpm

internal interface MpeLatencyReporter {
    fun onStart()

    fun onLoad(testName: String)
}
