package com.stripe.android.stripecardscan.cardimageverification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.Keep
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.stripecardscan.camera.CameraPreviewImage
import com.stripe.android.stripecardscan.cardimageverification.analyzer.MainLoopAnalyzer
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopAggregator
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopState
import com.stripe.android.stripecardscan.framework.AggregateResultListener
import com.stripe.android.stripecardscan.framework.AnalyzerLoopErrorListener
import com.stripe.android.stripecardscan.framework.AnalyzerPool
import com.stripe.android.stripecardscan.framework.ProcessBoundAnalyzerLoop
import com.stripe.android.stripecardscan.payment.ml.CardDetect
import com.stripe.android.stripecardscan.payment.ml.CardDetectModelManager
import com.stripe.android.stripecardscan.payment.ml.SSDOcr
import com.stripe.android.stripecardscan.payment.ml.SSDOcrModelManager
import com.stripe.android.stripecardscan.scanui.ScanFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Keep
internal data class SavedFrame(
    val hasOcr: Boolean,
    val frame: MainLoopAnalyzer.Input,
)

@Keep
internal data class SavedFrameType(
    val hasCard: Boolean,
    val hasOcr: Boolean,
)

@Keep
internal abstract class CardVerifyFlow(
    private val scanErrorListener: AnalyzerLoopErrorListener,
) : ScanFlow<RequiredCardDetails?>,
    AggregateResultListener<MainLoopAggregator.InterimResult, MainLoopAggregator.FinalResult> {

    /**
     * If this is true, do not start the flow.
     */
    private var canceled = false

    private var mainLoopAnalyzerPool: AnalyzerPool<
        MainLoopAnalyzer.Input,
        MainLoopState,
        MainLoopAnalyzer.Prediction>? = null
    private var mainLoopAggregator: MainLoopAggregator? = null
    private var mainLoop: ProcessBoundAnalyzerLoop<
        MainLoopAnalyzer.Input,
        MainLoopState,
        MainLoopAnalyzer.Prediction>? = null

    private var mainLoopJob: Job? = null

    override fun startFlow(
        context: Context,
        imageStream: Flow<CameraPreviewImage<Bitmap>>,
        viewFinder: Rect,
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope,
        parameters: RequiredCardDetails?,
    ) = coroutineScope.launch(Dispatchers.Main) {
        if (canceled) {
            return@launch
        }

        mainLoopAggregator = MainLoopAggregator(
            listener = this@CardVerifyFlow,
            requiredCardIssuer = parameters?.cardIssuer,
            requiredLastFour = parameters?.lastFour,
        ).also { mainLoopOcrAggregator ->
            // make this result aggregator pause and reset when the lifecycle pauses.
            mainLoopOcrAggregator.bindToLifecycle(lifecycleOwner)

            val analyzerPool = AnalyzerPool.of(
                MainLoopAnalyzer.Factory(
                    SSDOcr.Factory(
                        context,
                        SSDOcrModelManager.fetchModel(
                            context,
                            forImmediateUse = true,
                            isOptional = false
                        )
                    ),
                    CardDetect.Factory(
                        context,
                        CardDetectModelManager.fetchModel(
                            context,
                            forImmediateUse = true,
                            isOptional = false,
                        )
                    ),
                )
            )
            mainLoopAnalyzerPool = analyzerPool

            mainLoop = ProcessBoundAnalyzerLoop(
                analyzerPool = analyzerPool,
                resultHandler = mainLoopOcrAggregator,
                analyzerLoopErrorListener = scanErrorListener,
                statsName = null, // TODO: change this if we want to collect as part of scanstats
            ).apply {
                subscribeTo(
                    imageStream.map {
                        MainLoopAnalyzer.Input(
                            cameraPreviewImage = it,
                            cardFinder = viewFinder,
                        )
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

    override fun cancelFlow() {
        canceled = true

        mainLoopAggregator?.run { cancel() }
        mainLoopAggregator = null

        mainLoop?.unsubscribe()
        mainLoop = null

        mainLoopAnalyzerPool?.closeAllAnalyzers()
        mainLoopAnalyzerPool = null

        mainLoopJob?.apply { if (isActive) { cancel() } }
        mainLoopJob = null
    }

    /**
     * Select which frames to use in the completion loop.
     */
    fun <SavedFrame> selectCompletionLoopFrames(
        frames: Map<SavedFrameType, List<SavedFrame>>,
    ): Collection<SavedFrame> {
        fun getFrames(frameType: SavedFrameType) = frames[frameType] ?: emptyList()

        val cardAndPan = getFrames(SavedFrameType(hasCard = true, hasOcr = true))
        val pan = getFrames(SavedFrameType(hasCard = false, hasOcr = true))
        val card = getFrames(SavedFrameType(hasCard = true, hasOcr = false))

        return (cardAndPan + pan + card)
            .take(CardImageVerificationConfig.MAX_COMPLETION_LOOP_FRAMES)
    }
}
