package com.stripe.android.cardverificationsheet.cardverifyui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.Keep
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.cardverificationsheet.camera.CameraPreviewImage
import com.stripe.android.cardverificationsheet.cardverifyui.analyzer.MainLoopAnalyzer
import com.stripe.android.cardverificationsheet.cardverifyui.result.MainLoopAggregator
import com.stripe.android.cardverificationsheet.cardverifyui.result.MainLoopState
import com.stripe.android.cardverificationsheet.framework.AggregateResultListener
import com.stripe.android.cardverificationsheet.framework.AnalyzerLoopErrorListener
import com.stripe.android.cardverificationsheet.framework.AnalyzerPool
import com.stripe.android.cardverificationsheet.framework.ProcessBoundAnalyzerLoop
import com.stripe.android.cardverificationsheet.payment.ml.CardDetect
import com.stripe.android.cardverificationsheet.payment.ml.CardDetectModelManager
import com.stripe.android.cardverificationsheet.payment.ml.SSDOcr
import com.stripe.android.cardverificationsheet.payment.ml.SSDOcrModelManager
import com.stripe.android.cardverificationsheet.scanui.ScanFlow
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
internal class CardVerifyFlow(
    private val scanResultListener: AggregateResultListener<
        MainLoopAggregator.InterimResult,
        MainLoopAggregator.FinalResult>,
    private val scanErrorListener: AnalyzerLoopErrorListener,
) : ScanFlow<RequiredCardDetails?> {

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
        val listener = object : AggregateResultListener<
                MainLoopAggregator.InterimResult,
                MainLoopAggregator.FinalResult> {

            override suspend fun onResult(result: MainLoopAggregator.FinalResult) {
                mainLoop?.unsubscribe()
                mainLoop = null

                mainLoopJob?.apply { if (isActive) { cancel() } }
                mainLoopJob = null

                mainLoopAggregator = null

                mainLoopAnalyzerPool?.closeAllAnalyzers()
                mainLoopAnalyzerPool = null

                scanResultListener.onResult(result)
            }

            override suspend fun onInterimResult(result: MainLoopAggregator.InterimResult) {
                scanResultListener.onInterimResult(result)
            }

            override suspend fun onReset() {
                scanResultListener.onReset()
            }
        }

        if (canceled) {
            return@launch
        }

        mainLoopAggregator = MainLoopAggregator(
            listener = listener,
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

        return (cardAndPan + pan + card).take(VerifyConfig.MAX_COMPLETION_LOOP_FRAMES)
    }
}
