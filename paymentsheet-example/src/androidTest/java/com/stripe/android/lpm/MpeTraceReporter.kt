package com.stripe.android.lpm

import com.stripe.android.paymentsheet.state.PaymentSheetLoadTraceRecorder

internal class MpeTraceReporter : MpeLatencyReporter {
    override fun onStart() {
        PaymentSheetLoadTraceRecorder.startSession()
    }

    override fun onLoad(testName: String) {
        val trace = PaymentSheetLoadTraceRecorder.finishSession() ?: return

        println("${OUTPUT_PREFIX}|SESSION|$testName|${trace.totalDurationMs.formatMs()}")
        trace.spans.forEach { span ->
            println(
                "${OUTPUT_PREFIX}|SPAN|$testName|${span.name}|${span.startOffsetMs.formatMs()}|${span.durationMs.formatMs()}"
            )
        }
        System.out.flush()
    }

    private fun Double.formatMs(): String {
        return String.format("%.3f", this)
    }

    companion object {
        const val OUTPUT_PREFIX: String = "MPE_LOAD_TRACE"
    }
}
