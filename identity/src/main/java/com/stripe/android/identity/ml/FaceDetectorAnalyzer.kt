package com.stripe.android.identity.ml

import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.camera.framework.image.cropCenter
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.util.maxAspectRatioInSize
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.roundToMaxDecimals
import com.stripe.android.mlcore.base.InterpreterOptionsWrapper
import com.stripe.android.mlcore.base.InterpreterWrapper
import com.stripe.android.mlcore.impl.InterpreterWrapperImpl
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File

/**
 * Analyzer to run FaceDetector.
 */
internal class FaceDetectorAnalyzer(
    modelFile: File,
    private val modelPerformanceTracker: ModelPerformanceTracker
) : Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput> {

    private val interpreterApi: InterpreterWrapper = InterpreterWrapperImpl(
        modelFile,
        InterpreterOptionsWrapper.Builder().build()
    )

    override suspend fun analyze(
        data: AnalyzerInput,
        state: IdentityScanState
    ): AnalyzerOutput {
        val preprocessStat = modelPerformanceTracker.trackPreprocess()
        var tensorImage = TensorImage(INPUT_TENSOR_TYPE)
        val croppedImage = data.cameraPreviewImage.image.cropCenter(
            maxAspectRatioInSize(
                data.cameraPreviewImage.image.size(),
                1f
            )
        )

        tensorImage.load(croppedImage)

        // preprocess - resize the image to model input
        val imageProcessor =
            ImageProcessor.Builder()
                .add(
                    ResizeOp(INPUT_HEIGHT, INPUT_WIDTH, ResizeOp.ResizeMethod.BILINEAR)
                )
                .add(
                    NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD) // normalize to [0, 1)
                )
                .build()
        tensorImage = imageProcessor.process(tensorImage)
        preprocessStat.trackResult()

        val inferenceStat = modelPerformanceTracker.trackInference()
        // inference - input: (1, 128, 128, 3), output: (1, 4), (1, 1)
        val boundingBoxes = Array(1) { FloatArray(OUTPUT_BOUNDING_BOX_TENSOR_SIZE) }
        val score = FloatArray(OUTPUT_SCORE_TENSOR_SIZE)
        interpreterApi.runForMultipleInputsOutputs(
            arrayOf(tensorImage.buffer),
            mapOf(
                OUTPUT_BOUNDING_BOX_TENSOR_INDEX to boundingBoxes,
                OUTPUT_SCORE_TENSOR_INDEX to score
            )
        )
        inferenceStat.trackResult()

        // FaceDetector outputs (left, top, right, bottom) with absolute value
        // convert them to (left, top, width, height) with fractional value
        return FaceDetectorOutput(
            boundingBox = BoundingBox(
                left = boundingBoxes[0][0] / INPUT_WIDTH,
                top = boundingBoxes[0][1] / INPUT_HEIGHT,
                width = (boundingBoxes[0][2] - boundingBoxes[0][0]) / INPUT_WIDTH,
                height = (boundingBoxes[0][3] - boundingBoxes[0][1]) / INPUT_HEIGHT
            ),
            resultScore = score[0].roundToMaxDecimals(2)
        )
    }

    internal class Factory(
        private val modelFile: File,
        private val modelPerformanceTracker: ModelPerformanceTracker
    ) : AnalyzerFactory<
            AnalyzerInput,
            IdentityScanState,
            AnalyzerOutput,
            Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput>
            > {
        override suspend fun newInstance(): Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput> {
            return FaceDetectorAnalyzer(modelFile, modelPerformanceTracker)
        }
    }

    internal companion object {
        const val INPUT_WIDTH = 128
        const val INPUT_HEIGHT = 128
        const val OUTPUT_BOUNDING_BOX_TENSOR_INDEX = 0
        const val OUTPUT_SCORE_TENSOR_INDEX = 1
        const val OUTPUT_BOUNDING_BOX_TENSOR_SIZE = 4
        const val OUTPUT_SCORE_TENSOR_SIZE = 1
        const val NORMALIZE_MEAN = 0f
        const val NORMALIZE_STD = 255f

        val INPUT_TENSOR_TYPE: DataType = DataType.FLOAT32
        val TAG: String = FaceDetectorAnalyzer::class.java.simpleName

        const val MODEL_NAME = "face_detector_v1"
    }
}
