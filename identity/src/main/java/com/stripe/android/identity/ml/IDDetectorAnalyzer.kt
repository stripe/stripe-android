package com.stripe.android.identity.ml

import android.content.Context
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.camera.framework.image.cropCameraPreviewToSquare
import com.stripe.android.identity.states.IdentityScanState
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * Analyzer to run a model input.
 *
 * TODO(ccen): reimplement with ImageClassifier
 */
internal class IDDetectorAnalyzer(context: Context) :
    Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput> {

    private val tfliteInterpreter = Interpreter(
        context.assets.openFd(modelName).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { input ->
                input.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        }
    )

    override suspend fun analyze(data: AnalyzerInput, identityState: IdentityScanState): AnalyzerOutput {
        var tensorImage = TensorImage(INPUT_TENSOR_TYPE)
        val croppedImage = cropCameraPreviewToSquare(
            data.cameraPreviewImage.image,
            data.cameraPreviewImage.viewBounds,
            data.viewFinderBounds
        )

        tensorImage.load(croppedImage)

        // preprocess - resize the image to model input
        val imageProcessor =
            ImageProcessor.Builder().add(
                ResizeOp(INPUT_HEIGHT, INPUT_WIDTH, ResizeOp.ResizeMethod.BILINEAR)
            ).add(
                NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD) // normalize to (-1, 1)
            )
                .build() // add nomalization
        tensorImage = imageProcessor.process(tensorImage)

        // inference - input: (1, 224, 224, 3), output: (1, 4), (1, 5)
        val boundingBoxes = Array(1) { FloatArray(OUTPUT_BOUNDING_BOX_TENSOR_SIZE) }
        val categories = Array(1) { FloatArray(OUTPUT_CATEGORY_TENSOR_SIZE) }
        tfliteInterpreter.runForMultipleInputsOutputs(
            arrayOf(tensorImage.buffer),
            mapOf(
                OUTPUT_BOUNDING_BOX_TENSOR_INDEX to boundingBoxes,
                OUTPUT_CATEGORY_TENSOR_INDEX to categories,
            )
        )

        // find the category with highest score and build output
        val resultIndex = requireNotNull(categories[0].indices.maxByOrNull { categories[0][it] })

        val resultCategory: Category
        val resultScore: Float

        if (categories[0][resultIndex] > THRESHOLD) {
            resultCategory = requireNotNull(INDEX_CATEGORY_MAP[resultIndex])
            resultScore = categories[0][resultIndex]
        } else {
            resultCategory = Category.NO_ID
            resultScore = 0f
        }

        return AnalyzerOutput(
            BoundingBox(
                boundingBoxes[0][0],
                boundingBoxes[0][1],
                boundingBoxes[0][2],
                boundingBoxes[0][3],
            ),
            resultCategory,
            resultScore
        )
    }

    // TODO(ccen): check if we should enable this to track stats
    override val statsName: String? = null

    internal class Factory(
        private val context: Context
    ) : AnalyzerFactory<
            AnalyzerInput,
            IdentityScanState,
            AnalyzerOutput,
            Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput>
            > {
        override suspend fun newInstance(): Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput> {
            return IDDetectorAnalyzer(context)
        }
    }

    private companion object {
        const val INPUT_WIDTH = 224
        const val INPUT_HEIGHT = 224
        const val NORMALIZE_MEAN = 127.5f
        const val NORMALIZE_STD = 127.5f
        const val modelName = "2022IDDetectorWithoutMetadata.tflite"
        const val THRESHOLD = 0.4f
        const val OUTPUT_BOUNDING_BOX_TENSOR_INDEX = 0
        const val OUTPUT_CATEGORY_TENSOR_INDEX = 1
        const val OUTPUT_BOUNDING_BOX_TENSOR_SIZE = 4
        val INPUT_TENSOR_TYPE: DataType = DataType.FLOAT32
        val OUTPUT_CATEGORY_TENSOR_SIZE = Category.values().size
        val INDEX_CATEGORY_MAP = mapOf(
            0 to Category.NO_ID,
            1 to Category.PASSPORT,
            2 to Category.ID_FRONT,
            3 to Category.ID_BACK,
            4 to Category.INVALID,
        )
        val TAG: String = IDDetectorAnalyzer::class.java.simpleName
    }
}
