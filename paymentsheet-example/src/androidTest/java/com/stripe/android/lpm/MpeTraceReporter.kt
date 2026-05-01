package com.stripe.android.lpm

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
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
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0,
            Bundle().apply {
                putString(TRACE_KEY, line)
            }
        )
    }

    companion object {
        const val OUTPUT_PREFIX: String = "MPE_LOAD_TRACE"
        private const val TRACE_KEY: String = "mpe_load_trace"
    }
}
