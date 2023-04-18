package com.stripe.android.stripecardscan.payment.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.stripe.android.camera.framework.image.cropCameraPreviewToSquare
import com.stripe.android.camera.framework.image.hasOpenGl31
import com.stripe.android.camera.framework.image.scale
import com.stripe.android.mlcore.base.InterpreterOptionsWrapper
import com.stripe.android.mlcore.base.InterpreterWrapper
import com.stripe.android.stripecardscan.framework.FetchedData
import com.stripe.android.stripecardscan.framework.image.MLImage
import com.stripe.android.stripecardscan.framework.image.toMLImage
import com.stripe.android.stripecardscan.framework.ml.TFLAnalyzerFactory
import com.stripe.android.stripecardscan.framework.ml.TensorFlowLiteAnalyzer
import com.stripe.android.stripecardscan.framework.util.indexOfMax
import java.nio.ByteBuffer
import kotlin.math.max

private val TRAINED_IMAGE_SIZE = Size(224, 224)

/** model returns whether or not there is a card present */
private const val NUM_CLASS = 3

internal class CardDetect private constructor(interpreter: InterpreterWrapper) :
    TensorFlowLiteAnalyzer<
        CardDetect.Input,
        ByteBuffer,
        CardDetect.Prediction,
        Array<FloatArray>
        >(interpreter) {

    companion object {
        /**
         * Convert a camera preview image into a CardDetect input
         */
        fun cameraPreviewToInput(
            cameraPreviewImage: Bitmap,
            previewBounds: Rect,
            cardFinder: Rect
        ) = Input(
            cropCameraPreviewToSquare(cameraPreviewImage, previewBounds, cardFinder)
                .scale(TRAINED_IMAGE_SIZE)
                .toMLImage()
        )
    }

    data class Input(val cardDetectImage: MLImage)

    /**
     * A prediction returned by this analyzer.
     */
    data class Prediction(
        val side: Side,
        val noCardProbability: Float,
        val noPanProbability: Float,
        val panProbability: Float
    ) {
        val maxConfidence = max(max(noCardProbability, noPanProbability), panProbability)

        /**
         * Force a generic toString method to prevent leaking information about this class'
         * parameters after R8. Without this method, this `data class` will automatically generate a
         * toString which retains the original names of the parameters even after obfuscation.
         */
        override fun toString(): String {
            return "Prediction"
        }

        enum class Side {
            NO_CARD,
            NO_PAN,
            PAN
        }
    }

    override suspend fun interpretMLOutput(data: Input, mlOutput: Array<FloatArray>): Prediction {
        val side = when (val index = mlOutput[0].indexOfMax()) {
            0 -> Prediction.Side.NO_PAN
            1 -> Prediction.Side.NO_CARD
            2 -> Prediction.Side.PAN
            else -> throw EnumConstantNotPresentException(
                Prediction.Side::class.java,
                index.toString()
            )
        }

        return Prediction(
            side = side,
            noPanProbability = mlOutput[0][0],
            noCardProbability = mlOutput[0][1],
            panProbability = mlOutput[0][2]
        )
    }

    override suspend fun transformData(data: Input): ByteBuffer =
        data.cardDetectImage.getData()

    override suspend fun executeInference(
        tfInterpreter: InterpreterWrapper,
        data: ByteBuffer
    ): Array<FloatArray> {
        val mlOutput = arrayOf(FloatArray(NUM_CLASS))
        tfInterpreter.run(data, mlOutput)
        return mlOutput
    }

    /**
     * A factory for creating instances of this analyzer.
     */
    class Factory(
        context: Context,
        fetchedModel: FetchedData,
        threads: Int = DEFAULT_THREADS
    ) : TFLAnalyzerFactory<Input, Prediction, CardDetect>(context, fetchedModel) {
        companion object {
            private const val USE_GPU = false
            private const val DEFAULT_THREADS = 4
        }

        override val tfOptions = InterpreterOptionsWrapper.Builder()
            .useNNAPI(USE_GPU && hasOpenGl31(context.applicationContext))
            .numThreads(threads)
            .build()

        override suspend fun newInstance(): CardDetect? =
            createInterpreter()?.let { CardDetect(it) }
    }
}
