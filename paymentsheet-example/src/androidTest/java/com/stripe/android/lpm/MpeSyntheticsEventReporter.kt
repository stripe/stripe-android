package com.stripe.android.lpm

import com.stripe.android.paymentsheet.analytics.MpeLatencyCapture

internal class MpeSyntheticsEventReporter : MpeLatencyReporter {
    override fun onStart(testName: String) {
        MpeLatencyCapture.registerSynthetics(testName)
    }

    override fun onLoad(testName: String) = Unit
}
