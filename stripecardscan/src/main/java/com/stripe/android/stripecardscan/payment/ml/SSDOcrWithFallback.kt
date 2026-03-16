package com.stripe.android.stripecardscan.payment.ml

import android.content.Context
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.stripecardscan.framework.FetchedData
import java.io.Closeable

/**
 * A composite analyzer that wraps an optional primary analyzer with a
 * fallback analyzer. If the primary returns a null prediction (or was
 * never loaded), the fallback is used instead.
 */
internal class SSDOcrWithFallback(
    private val primary: Analyzer<SSDOcr.Input, Any, SSDOcr.Prediction>?,
    private val fallback: Analyzer<SSDOcr.Input, Any, SSDOcr.Prediction>
) : Analyzer<SSDOcr.Input, Any, SSDOcr.Prediction>, Closeable {

    override suspend fun analyze(data: SSDOcr.Input, state: Any): SSDOcr.Prediction {
        if (primary != null) {
            val result = primary.analyze(data, state)
            if (result.pan != null) {
                val expiry = (fallback as? MLKitTextRecognizer)?.analyzeForExpiryOnly(data)
                return result.copy(expiry = expiry)
            }
        }
        return fallback.analyze(data, state)
    }

    override fun close() {
        (primary as? Closeable)?.close()
        // Do NOT close fallback here - the Factory owns it and
        // shares it across all pool instances.
    }

    /**
     * Factory that creates [SSDOcrWithFallback] instances. Creates SSDOcr via
     * the existing [SSDOcr.Factory], and shares a single [MLKitTextRecognizer]
     * across all instances (it is thread-safe).
     *
     * [newInstance] always returns non-null, even if SSDOcr fails to load,
     * ensuring [com.stripe.android.camera.framework.AnalyzerPool.of] always
     * gets analyzers (it uses mapNotNull internally).
     */
    class Factory(
        context: Context,
        fetchedModel: FetchedData,
        threads: Int = DEFAULT_THREADS
    ) : AnalyzerFactory<SSDOcr.Input, Any, SSDOcr.Prediction, SSDOcrWithFallback>, Closeable {

        private val ssdOcrFactory = SSDOcr.Factory(context, fetchedModel, threads)
        private val sharedMlKitRecognizer = MLKitTextRecognizer(
            MLKitTextRecognizer.createTextRecognizer()
        )

        override suspend fun newInstance(): SSDOcrWithFallback {
            val ssdOcr = ssdOcrFactory.newInstance()
            return SSDOcrWithFallback(ssdOcr, sharedMlKitRecognizer)
        }

        override fun close() {
            sharedMlKitRecognizer.close()
        }

        companion object {
            private const val DEFAULT_THREADS = 4
        }
    }
}
