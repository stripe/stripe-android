package com.stripe.android.identity.ml

import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.camera.framework.image.cropCenter
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.util.maxAspectRatioInSize
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.roundToMaxDecimals
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File

/**
 * Analyzer to run IDDetector.
 *
 * TODO(ccen): reimplement with ImageClassifier
 */
internal class IDDetectorAnalyzer(modelFile: File, private val idDetectorMinScore: Float) :
    Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput> {

    private val tfliteInterpreter = Interpreter(modelFile)

    override suspend fun analyze(
        data: AnalyzerInput,
        state: IdentityScanState
    ): AnalyzerOutput {
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
                NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD) // normalize to (-1, 1)
            ).build() // add normalization
        tensorImage = imageProcessor.process(tensorImage)

        // inference - input: (1, 224, 224, 3), output: (1, 4), (1, 5)
        val boundingBoxes = Array(1) { FloatArray(OUTPUT_BOUNDING_BOX_TENSOR_SIZE) }
        val categories = Array(1) { FloatArray(OUTPUT_CATEGORY_TENSOR_SIZE) }
        tfliteInterpreter.runForMultipleInputsOutputs(
            arrayOf(tensorImage.buffer),
            mapOf(
                OUTPUT_BOUNDING_BOX_TENSOR_INDEX to boundingBoxes,
                OUTPUT_CATEGORY_TENSOR_INDEX to categories
            )
        )

        // find the category with highest score and build output
        val resultIndex = requireNotNull(categories[0].indices.maxByOrNull { categories[0][it] })

        val resultCategory: Category
        val resultScore: Float

        // TODO(ccen) use idDetectorMinScore when server updates the value
        if (categories[0][resultIndex] > THRESHOLD) {
            resultCategory = requireNotNull(INDEX_CATEGORY_MAP[resultIndex])
            resultScore = categories[0][resultIndex]
        } else {
            resultCategory = Category.NO_ID
            resultScore = 0f
        }

        return IDDetectorOutput(
            BoundingBox(
                left = boundingBoxes[0][0],
                top = boundingBoxes[0][1],
                width = boundingBoxes[0][2],
                height = boundingBoxes[0][3]
            ),
            resultCategory,
            resultScore,
            categories[0].map { it.roundToMaxDecimals(2) }
        )
    }

    // TODO(ccen): check if we should enable this to track stats
    override val statsName: String? = null

    internal class Factory(
        private val modelFile: File,
        private val idDetectorMinScore: Float
    ) : AnalyzerFactory<
            AnalyzerInput,
            IdentityScanState,
            AnalyzerOutput,
            Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput>
            > {
        override suspend fun newInstance(): Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput> {
            return IDDetectorAnalyzer(modelFile, idDetectorMinScore)
        }
    }

    internal companion object {
        const val INPUT_WIDTH = 224
        const val INPUT_HEIGHT = 224
        const val NORMALIZE_MEAN = 127.5f
        const val NORMALIZE_STD = 127.5f
        const val THRESHOLD = 0.4f
        const val OUTPUT_BOUNDING_BOX_TENSOR_INDEX = 0
        const val OUTPUT_CATEGORY_TENSOR_INDEX = 1
        const val OUTPUT_BOUNDING_BOX_TENSOR_SIZE = 4
        private const val INDEX_NO_ID = 0
        const val INDEX_PASSPORT = 1
        const val INDEX_ID_FRONT = 2
        const val INDEX_ID_BACK = 3
        const val INDEX_INVALID = 4
        val INPUT_TENSOR_TYPE: DataType = DataType.FLOAT32
        val OUTPUT_CATEGORY_TENSOR_SIZE = Category.values().size
        val INDEX_CATEGORY_MAP = mapOf(
            INDEX_NO_ID to Category.NO_ID,
            INDEX_PASSPORT to Category.PASSPORT,
            INDEX_ID_FRONT to Category.ID_FRONT,
            INDEX_ID_BACK to Category.ID_BACK,
            INDEX_INVALID to Category.INVALID
        )
        val TAG: String = IDDetectorAnalyzer::class.java.simpleName
    }
}
