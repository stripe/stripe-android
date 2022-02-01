package com.stripe.android.identity.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.AnalyzerPool
import com.stripe.android.camera.framework.ProcessBoundAnalyzerLoop
import com.stripe.android.camera.scanui.ScanFlow
import com.stripe.android.identity.ml.IDDetectorAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class IdentityScanFlow :
    ScanFlow<Int, CameraPreviewImage<Bitmap>> {

    private var loop:
        ProcessBoundAnalyzerLoop<
            IDDetectorAnalyzer.Input,
            IDDetectorAnalyzer.State,
            IDDetectorAnalyzer.Output
            >? = null

    private var aggregator = IDDetectorAggregator()

    private var analyzerPool:
        AnalyzerPool<
            IDDetectorAnalyzer.Input,
            IDDetectorAnalyzer.State,
            IDDetectorAnalyzer.Output
            >? = null

    override fun startFlow(
        context: Context,
        imageStream: Flow<CameraPreviewImage<Bitmap>>,
        viewFinder: Rect,
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope,
        parameters: Int
    ) {
        coroutineScope.launch {
            aggregator.bindToLifecycle(lifecycleOwner)

            analyzerPool = AnalyzerPool.of(
                IDDetectorAnalyzer.Factory(context)
            )

            loop = ProcessBoundAnalyzerLoop(
                analyzerPool = requireNotNull(analyzerPool),
                resultHandler = aggregator,
                analyzerLoopErrorListener = IDDetectorAnalyzerLoopErrorListener(),
                statsName = null // TODO(ccen): determine if we need this
            )

            requireNotNull(loop).subscribeTo(
                imageStream.map { cameraPreviewImage ->
                    IDDetectorAnalyzer.Input(cameraPreviewImage, viewFinder)
                },
                coroutineScope,
            )
        }
    }

    override fun cancelFlow() {
    }
}
