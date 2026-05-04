package com.stripe.android.lpm

import com.stripe.android.paymentsheet.analytics.MpeLatencyCapture

internal class MpeBenchmarkEventReporter : MpeLatencyReporter {
    override fun onStart(testName: String) {
        MpeLatencyCapture.registerBenchmark(testName)
    }

    override fun onLoad(testName: String) = Unit
}
