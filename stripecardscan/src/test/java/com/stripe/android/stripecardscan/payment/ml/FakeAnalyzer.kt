package com.stripe.android.stripecardscan.payment.ml

import app.cash.turbine.Turbine
import com.stripe.android.camera.framework.Analyzer
import java.io.Closeable

/**
 * A fake [Analyzer] for testing composite analyzers like [SSDOcrWithFallback].
 * Returns a configurable [SSDOcr.Prediction] and tracks analyze calls via Turbine.
 */
internal class FakeAnalyzer(
    var prediction: SSDOcr.Prediction = SSDOcr.Prediction(null)
) : Analyzer<SSDOcr.Input, Any, SSDOcr.Prediction>, Closeable {

    val analyzeCalls = Turbine<SSDOcr.Input>()
    var closed = false
        private set

    override suspend fun analyze(data: SSDOcr.Input, state: Any): SSDOcr.Prediction {
        analyzeCalls.add(data)
        return prediction
    }

    override fun close() {
        closed = true
    }

    fun ensureAllEventsConsumed() {
        analyzeCalls.ensureAllEventsConsumed()
    }
}
