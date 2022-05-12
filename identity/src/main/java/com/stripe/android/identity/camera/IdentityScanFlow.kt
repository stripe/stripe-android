package com.stripe.android.identity.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.camera.framework.AnalyzerPool
import com.stripe.android.camera.framework.ProcessBoundAnalyzerLoop
import com.stripe.android.camera.scanui.ScanFlow
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.IDDetectorAnalyzer
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.IdentityScanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

/**
 * Identity's [ScanFlow] implementation, uses a pool of [IDDetectorAnalyzer] to within a
 * [ProcessBoundAnalyzerLoop] to analyze a [Flow] of [CameraPreviewImage]s.
 * The results are handled in [IdentityAggregator].
 *
 * TODO(ccen): merge with [CardScanFlow].
 */
internal class IdentityScanFlow(
    private val analyzerLoopErrorListener: AnalyzerLoopErrorListener,
    private val aggregateResultListener: AggregateResultListener<IdentityAggregator.InterimResult, IdentityAggregator.FinalResult>,
    private val idDetectorModelFile: File,
    private val verificationPage: VerificationPage
) : ScanFlow<IdentityScanState.ScanType, CameraPreviewImage<Bitmap>> {
    private var aggregator: IdentityAggregator? = null

    /**
     * If this is true, do not start the flow.
     */
    private var canceled = false

    /**
     * Pool of analyzers, initialized when [startFlow] is called.
     */
    private var analyzerPool:
        AnalyzerPool<
            AnalyzerInput,
            IdentityScanState,
            AnalyzerOutput
            >? = null

    /**
     * The loop to execute analyze, initialized upon [analyzerPool] is initialized.
     */
    private var loop:
        ProcessBoundAnalyzerLoop<
            AnalyzerInput,
            IdentityScanState,
            AnalyzerOutput
            >? = null

    /**
     * The [Job] to track loop, initialized upon [loop] starts.
     */
    private var loopJob: Job? = null

    override fun startFlow(
        context: Context,
        imageStream: Flow<CameraPreviewImage<Bitmap>>,
        viewFinder: Rect,
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope,
        parameters: IdentityScanState.ScanType
    ) {
        coroutineScope.launch {
            if (canceled) {
                return@launch
            }
            aggregator = IdentityAggregator(
                parameters,
                aggregateResultListener,
                verificationPage
            )

            requireNotNull(aggregator).bindToLifecycle(lifecycleOwner)

            analyzerPool = AnalyzerPool.of(
                IDDetectorAnalyzer.Factory(
                    idDetectorModelFile,
                    verificationPage.documentCapture.models.idDetectorMinScore
                )
            )

            loop = ProcessBoundAnalyzerLoop(
                analyzerPool = requireNotNull(analyzerPool),
                resultHandler = requireNotNull(aggregator),
                analyzerLoopErrorListener = analyzerLoopErrorListener
            )

            loopJob = requireNotNull(loop).subscribeTo(
                imageStream.map { cameraPreviewImage ->
                    AnalyzerInput(cameraPreviewImage, viewFinder)
                },
                coroutineScope,
            )
        }
    }

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
        aggregator?.run { cancel() }
        aggregator = null

        loop?.unsubscribe()
        loop = null

        analyzerPool?.closeAllAnalyzers()
        analyzerPool = null

        loopJob?.apply {
            if (isActive) {
                cancel()
            }
        }
        loopJob = null
    }
}
