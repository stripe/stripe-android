package com.stripe.android.paymentsheet.analytics

import android.os.Build
import android.os.Trace
import java.util.concurrent.atomic.AtomicBoolean

/** Perfetto boundary for release benchmarks; no analytics or clock work when tracing is disabled. */
internal class PaymentSheetFirstFrameTrace {
    private val pendingDraw = AtomicBoolean(false)
    private val cookie = System.identityHashCode(this)

    fun onLoadComplete() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Trace.isEnabled()) {
            Trace.beginAsyncSection(SECTION_NAME, cookie)
            pendingDraw.set(true)
        }
    }

    fun onDrawn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pendingDraw.compareAndSet(true, false)) {
            Trace.endAsyncSection(SECTION_NAME, cookie)
        }
    }

    private companion object {
        const val SECTION_NAME = "PaymentSheet#loadToFirstDraw"
    }
}
