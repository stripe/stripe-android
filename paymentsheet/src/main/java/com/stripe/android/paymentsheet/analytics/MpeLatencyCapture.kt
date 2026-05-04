package com.stripe.android.paymentsheet.analytics

import java.util.concurrent.atomic.AtomicReference

public object MpeLatencyCapture {
    private val currentConfig = AtomicReference<Config?>(null)

    public fun registerBenchmark(testName: String) {
        currentConfig.set(Config(testName = testName, mode = Mode.Benchmark))
    }

    public fun registerSynthetics(testName: String) {
        currentConfig.set(Config(testName = testName, mode = Mode.Synthetics))
    }

    public fun consume(): Config? {
        return currentConfig.getAndSet(null)
    }

    public fun clear() {
        currentConfig.set(null)
    }

    public data class Config(
        val testName: String,
        val mode: Mode,
    )

    public enum class Mode {
        Benchmark,
        Synthetics,
    }
}
