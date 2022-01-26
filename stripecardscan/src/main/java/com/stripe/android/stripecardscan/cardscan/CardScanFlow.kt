package com.stripe.android.stripecardscan.cardscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator
import com.stripe.android.stripecardscan.cardscan.result.MainLoopState
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.camera.framework.AnalyzerPool
import com.stripe.android.camera.framework.ProcessBoundAnalyzerLoop
import com.stripe.android.stripecardscan.payment.ml.SSDOcr
import com.stripe.android.stripecardscan.payment.ml.SSDOcrModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal data class SavedFrame(
    val hasOcr: Boolean,
    val frame: SSDOcr.Input,
)

internal data class SavedFrameType(
    val hasOcr: Boolean,
)

internal abstract class CardScanFlow(
    private val scanErrorListener: AnalyzerLoopErrorListener,
) : AggregateResultListener<MainLoopAggregator.InterimResult, MainLoopAggregator.FinalResult> {

    /**
     * If this is true, do not start the flow.
     */
    private var canceled = false

    private var mainLoopAnalyzerPool: AnalyzerPool<
        SSDOcr.Input,
        Any,
        SSDOcr.Prediction>? = null
    private var mainLoopAggregator: MainLoopAggregator? = null
    private var mainLoop: ProcessBoundAnalyzerLoop<
        SSDOcr.Input,
        MainLoopState,
        SSDOcr.Prediction>? = null

    private var mainLoopJob: Job? = null

    fun startFlow(
        context: Context,
        imageStream: Flow<CameraPreviewImage<Bitmap>>,
        viewFinder: Rect,
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope
    ) = coroutineScope.launch(Dispatchers.Main) {
        if (canceled) {
            return@launch
        }

        mainLoopAggregator = MainLoopAggregator(
            listener = this@CardScanFlow
        ).also {
            // make this result aggregator pause and reset when the lifecycle pauses.
            it.bindToLifecycle(lifecycleOwner)

            val analyzerPool = AnalyzerPool.of(
                SSDOcr.Factory(
                    context,
                    SSDOcrModelManager.fetchModel(
                        context,
                        forImmediateUse = true,
                        isOptional = false
                    )
                )
            )
            mainLoopAnalyzerPool = analyzerPool

            mainLoop = ProcessBoundAnalyzerLoop(
                analyzerPool = analyzerPool,
                resultHandler = it,
                analyzerLoopErrorListener = scanErrorListener,
                statsName = null, // TODO: change this if we want to collect as part of scanstats
            ).apply {
                subscribeTo(
                    imageStream.map {
                        SSDOcr.cameraPreviewToInput(it.image, it.viewBounds, viewFinder)
                    },
                    coroutineScope,
                )
            }
        }
    }.let { }

    override suspend fun onResult(result: MainLoopAggregator.FinalResult) {
        mainLoop?.unsubscribe()
        mainLoop = null

        mainLoopJob?.apply { if (isActive) { cancel() } }
        mainLoopJob = null

        mainLoopAggregator = null

        mainLoopAnalyzerPool?.closeAllAnalyzers()
        mainLoopAnalyzerPool = null
    }
}
