package com.stripe.android.lpm

import android.util.Log
import com.stripe.android.paymentsheet.state.PaymentSheetLoadTraceRecorder

internal class MpeTraceReporter : MpeLatencyReporter {
    override fun onStart() {
        PaymentSheetLoadTraceRecorder.startSession()
    }

    override fun onLoad(testName: String) {
        val trace = PaymentSheetLoadTraceRecorder.finishSession() ?: return

        emitTraceLine("${OUTPUT_PREFIX}|SESSION|$testName|${trace.totalDurationMs.formatMs()}")
        trace.spans.forEach { span ->
            emitTraceLine(
                "${OUTPUT_PREFIX}|SPAN|$testName|${span.name}|${span.startOffsetMs.formatMs()}|${span.durationMs.formatMs()}"
            )
        }
    }

    private fun Double.formatMs(): String {
        return String.format("%.3f", this)
    }

    private fun emitTraceLine(line: String) {
        Log.i(LOG_TAG, line)
    }

    companion object {
        const val OUTPUT_PREFIX: String = "MPE_LOAD_TRACE"
        private const val LOG_TAG: String = "MPELoadTrace"
    }
}
