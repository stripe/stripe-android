package com.stripe.android.lpm

import android.util.Log
import com.stripe.android.core.utils.DurationProvider

internal class MpeBenchmarkEventReporter(
    private val durationProvider: DurationProvider,
) : MpeLatencyReporter {
    override fun onStart() {
        durationProvider.start(DurationProvider.Key.MpeSynthetics)
    }

    override fun onLoad(testName: String) {
        val duration = durationProvider.end(DurationProvider.Key.MpeSynthetics)
        val durationMs = duration?.inWholeMilliseconds ?: return

        Log.i(
            LOG_TAG,
            "SYNTHETIC_LATENCY_RESULT: $testName: $durationMs"
        )
    }

    companion object {
        const val LOG_TAG: String = "MPELatencyBenchmark"
    }
}
