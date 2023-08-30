package com.stripe.android.identity.ml

import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.camera.framework.image.cropCenter
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.util.maxAspectRatioInSize
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.LaplacianBlurDetector
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
 * Analyzer to run IDDetector.
 */
internal class IDDetectorAnalyzer(
    modelFile: File,
    private val idDetectorMinScore: Float,
    private val modelPerformanceTracker: ModelPerformanceTracker,
    private val laplacianBlurDetector: LaplacianBlurDetector,
) :
    Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput> {

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
            ImageProcessor.Builder().add(
                ResizeOp(INPUT_HEIGHT, INPUT_WIDTH, ResizeOp.ResizeMethod.BILINEAR)
            ).add(
                NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD) // normalize to [0, 1)
            ).build() // add normalization
        tensorImage = imageProcessor.process(tensorImage)
        preprocessStat.trackResult()

        val inferenceStat = modelPerformanceTracker.trackInference()
        // inference - input: (1, 224, 224, 3), output: (392, 4), (392, 4)
        val boundingBoxes = Array(OUTPUT_SIZE) { FloatArray(OUTPUT_BOUNDING_BOX_TENSOR_SIZE) }
        val categories = Array(OUTPUT_SIZE) { FloatArray(OUTPUT_CATEGORY_TENSOR_SIZE) }
        interpreterApi.runForMultipleInputsOutputs(
            arrayOf(tensorImage.buffer),
            mapOf(
                OUTPUT_BOUNDING_BOX_TENSOR_INDEX to boundingBoxes,
                OUTPUT_CATEGORY_TENSOR_INDEX to categories
            )
        )
        inferenceStat.trackResult()

        // To get more results, run nonMaxSuppressionMultiClass on the categories.
        // Fut for IDDetector, we just need to find the highest score and return it's
        // corresponding box.
        var bestIndex = 0
        var bestScore = Float.MIN_VALUE
        var bestCategoryIndex = INDEX_INVALID

        // Find the best score in the output 2d array of (392, 4),
        // return its index within range [0, 392) on 1d as bestIndex.
        for (currentOutputIndex in 0 until OUTPUT_SIZE) {
            val currentScores = categories[currentOutputIndex]
            val currentBestCategoryIndex = currentScores.indices.maxBy {
                currentScores[it]
            }
            val currentBestScore = currentScores[currentBestCategoryIndex]
            if (bestScore < currentBestScore && currentBestScore > idDetectorMinScore) {
                bestScore = currentBestScore
                bestIndex = currentOutputIndex
                bestCategoryIndex = currentBestCategoryIndex
            }
        }
        val bestCategory = INDEX_CATEGORY_MAP[bestCategoryIndex] ?: Category.INVALID
        val bestBoundingBox = boundingBoxes[bestIndex]
        return IDDetectorOutput(
            BoundingBox(
                bestBoundingBox[0],
                bestBoundingBox[1],
                bestBoundingBox[2],
                bestBoundingBox[3]
            ),
            bestCategory,
            bestScore,
            LIST_OF_INDICES.map {
                categories[bestIndex][it].roundToMaxDecimals(2)
            },
            laplacianBlurDetector.calculateBlurOutput(croppedImage)
        )
    }

    internal class Factory(
        private val modelFile: File,
        private val idDetectorMinScore: Float,
        private val modelPerformanceTracker: ModelPerformanceTracker,
        private val laplacianBlurDetector: LaplacianBlurDetector
    ) : AnalyzerFactory<
            AnalyzerInput,
            IdentityScanState,
            AnalyzerOutput,
            Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput>
            > {
        override suspend fun newInstance(): Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput> {
            return IDDetectorAnalyzer(
                modelFile,
                idDetectorMinScore,
                modelPerformanceTracker,
                laplacianBlurDetector
            )
        }
    }

    internal companion object {
        const val OUTPUT_SIZE = 392

        const val INPUT_WIDTH = 224
        const val INPUT_HEIGHT = 224

        // (0, 1)
        const val NORMALIZE_MEAN = 0f
        const val NORMALIZE_STD = 255f
        const val OUTPUT_BOUNDING_BOX_TENSOR_INDEX = 0
        const val OUTPUT_CATEGORY_TENSOR_INDEX = 1
        const val OUTPUT_BOUNDING_BOX_TENSOR_SIZE = 4
        const val INDEX_PASSPORT = 0
        const val INDEX_ID_FRONT = 1
        const val INDEX_ID_BACK = 2
        const val INDEX_INVALID = 3
        val LIST_OF_INDICES = listOf(
            INDEX_PASSPORT,
            INDEX_ID_FRONT,
            INDEX_ID_BACK,
            INDEX_INVALID
        )
        val INPUT_TENSOR_TYPE: DataType = DataType.FLOAT32
        val OUTPUT_CATEGORY_TENSOR_SIZE = Category.values().size - 1 // no NO_ID
        val INDEX_CATEGORY_MAP = mapOf(
            INDEX_PASSPORT to Category.PASSPORT,
            INDEX_ID_FRONT to Category.ID_FRONT,
            INDEX_ID_BACK to Category.ID_BACK,
            INDEX_INVALID to Category.INVALID
        )
        val TAG: String = IDDetectorAnalyzer::class.java.simpleName

        const val MODEL_NAME = "id_detector_v2"
    }
}
