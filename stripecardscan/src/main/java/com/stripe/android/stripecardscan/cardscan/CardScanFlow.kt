package com.stripe.android.stripecardscan.cardscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.camera.framework.AnalyzerPool
import com.stripe.android.camera.framework.ProcessBoundAnalyzerLoop
import com.stripe.android.camera.framework.util.centerOn
import com.stripe.android.camera.framework.util.minAspectRatioSurroundingSize
import com.stripe.android.camera.framework.util.size
import com.stripe.android.camera.framework.util.union
import com.stripe.android.camera.scanui.ScanFlow
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator
import com.stripe.android.stripecardscan.cardscan.result.MainLoopState
import com.stripe.android.stripecardscan.payment.ml.CardOcr
import com.stripe.android.stripecardscan.payment.ml.MLKitTextRecognizer
import com.stripe.android.stripecardscan.payment.ml.SSDOcr
import com.stripe.android.stripecardscan.payment.ml.SSDOcrModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal abstract class CardScanFlow(
    private val scanErrorListener: AnalyzerLoopErrorListener,
    private val enableMlKitTextRecognition: Boolean = false,
) : ScanFlow<Unit?, CameraPreviewImage<Bitmap>>,
    AggregateResultListener<MainLoopAggregator.InterimResult, MainLoopAggregator.FinalResult> {

    /**
     * If this is true, do not start the flow.
     */
    private var canceled = false

    private var mainLoopAggregator: MainLoopAggregator? = null

    private val analyzerLoops = mutableListOf<AnalyzerLoop>()

    private data class AnalyzerLoop(
        val pool: AnalyzerPool<CardOcr.Input, Any, CardOcr.Prediction>,
        val loop: ProcessBoundAnalyzerLoop<CardOcr.Input, MainLoopState, CardOcr.Prediction>,
        val job: Job,
    )

    override fun startFlow(
        context: Context,
        imageStream: Flow<CameraPreviewImage<Bitmap>>,
        viewFinder: Rect,
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope,
        parameters: Unit?,
        errorHandler: (e: Exception) -> Unit
    ) = coroutineScope.launch(Dispatchers.Main) {
        if (canceled) {
            return@launch
        }

        val aggregator = MainLoopAggregator(
            listener = this@CardScanFlow
        ).also { it.bindToLifecycle(lifecycleOwner) }
        mainLoopAggregator = aggregator

        val inputStream = imageStream.map { cameraImage ->
            CardOcr.cameraPreviewToInput(
                cameraImage.image,
                minAspectRatioSurroundingSize(
                    cameraImage.viewBounds.size().union(viewFinder.size()),
                    cameraImage.image.width.toFloat() / cameraImage.image.height
                ).centerOn(cameraImage.viewBounds),
                viewFinder
            )
        }

        val fetchedModel = SSDOcrModelManager.fetchModel(
            context,
            forImmediateUse = true,
            isOptional = false
        )
        // SSD loop: uses a built-in model, runs ~3x faster than ML Kit
        createAndRegisterLoop(
            AnalyzerPool.of(SSDOcr.Factory(context, fetchedModel)),
            aggregator, inputStream, coroutineScope,
        )

        // ML Kit loop: Uses generic OCR, much slower (~2 fps) but potentially
        // catches more cards, can detect expiration dates
        if (enableMlKitTextRecognition) {
            createAndRegisterLoop(
                AnalyzerPool.of(MLKitTextRecognizer.Factory(), desiredAnalyzerCount = 1),
                aggregator, inputStream, coroutineScope,
            )
        }
    }.let { }

    /**
     * Reset the flow to the initial state, ready to be started again
     */
    internal fun resetFlow() {
        canceled = false
        cleanUp()
    }

    override fun cancelFlow() {
        canceled = true
        cleanUp()
    }

    private fun cleanUp() {
        mainLoopAggregator?.cancel()
        mainLoopAggregator = null

        analyzerLoops.forEach { it.loop.unsubscribe() }

        // Wait for the above unsubscribe() calls to take effect
        // before we closeAllAnalyzers(), or we might inadvertently
        // deallocate a worker while it's processing
        runBlocking {
            analyzerLoops.forEach { it.job.join() }
        }

        analyzerLoops.forEach { it.pool.closeAllAnalyzers() }
        analyzerLoops.clear()
    }

    /**
     * Collect analytics data from a completed scan with a final result.
     */
    internal fun collectAnalyticsData(
        finalResult: MainLoopAggregator.FinalResult
    ): CardScanAnalyticsData {
        val aggregator = mainLoopAggregator
        return CardScanAnalyticsData().apply {
            mlKitEnabled = enableMlKitTextRecognition
            totalFramesProcessed = aggregator?.getTotalFramesProcessed() ?: 0
            averageFrameRateHz = aggregator?.getAverageFrameRateHz()
            panFound = true
            expiryFound = finalResult.expiryFound
            highestPanAgreement = finalResult.highestPanAgreement
            finishReason = finalResult.finishReason
            stateResetCount = aggregator?.stateResetCount ?: 0
            timeToFirstDetectionMs = aggregator?.timeToFirstDetectionMs
        }
    }

    /**
     * Collect partial analytics data when scan ends without a final result (failure/cancel).
     */
    internal fun collectPartialAnalyticsData(): CardScanAnalyticsData {
        val aggregator = mainLoopAggregator
        return CardScanAnalyticsData().apply {
            mlKitEnabled = enableMlKitTextRecognition
            totalFramesProcessed = aggregator?.getTotalFramesProcessed() ?: 0
            averageFrameRateHz = aggregator?.getAverageFrameRateHz()
            stateResetCount = aggregator?.stateResetCount ?: 0
            timeToFirstDetectionMs = aggregator?.timeToFirstDetectionMs
        }
    }

    private fun createAndRegisterLoop(
        pool: AnalyzerPool<CardOcr.Input, Any, CardOcr.Prediction>,
        aggregator: MainLoopAggregator,
        inputStream: Flow<CardOcr.Input>,
        coroutineScope: CoroutineScope,
    ) {
        val loop = ProcessBoundAnalyzerLoop(
            analyzerPool = pool,
            resultHandler = aggregator,
            analyzerLoopErrorListener = scanErrorListener,
        )
        val job = loop.subscribeTo(inputStream, coroutineScope)
        if (job != null) {
            analyzerLoops.add(AnalyzerLoop(pool, loop, job))
        }
    }
}
