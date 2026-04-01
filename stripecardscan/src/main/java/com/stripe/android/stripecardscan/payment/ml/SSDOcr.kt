package com.stripe.android.stripecardscan.payment.ml

import android.content.Context
import android.util.Size
import androidx.annotation.VisibleForTesting
import com.stripe.android.camera.framework.image.hasOpenGl31
import com.stripe.android.mlcore.base.InterpreterOptionsWrapper
import com.stripe.android.mlcore.base.InterpreterWrapper
import com.stripe.android.stripecardscan.framework.FetchedData
import com.stripe.android.stripecardscan.framework.ml.TFLAnalyzerFactory
import com.stripe.android.stripecardscan.framework.ml.TensorFlowLiteAnalyzer
import com.stripe.android.stripecardscan.framework.ml.ssd.adjustLocations
import com.stripe.android.stripecardscan.framework.ml.ssd.softMax
import com.stripe.android.stripecardscan.framework.ml.ssd.toRectForm
import com.stripe.android.stripecardscan.framework.util.reshape
import com.stripe.android.stripecardscan.payment.card.isValidPan
import com.stripe.android.stripecardscan.payment.ml.ssd.OcrFeatureMapSizes
import com.stripe.android.stripecardscan.payment.ml.ssd.combinePriors
import com.stripe.android.stripecardscan.payment.ml.ssd.determineLayoutAndFilter
import com.stripe.android.stripecardscan.payment.ml.ssd.extractPredictions
import com.stripe.android.stripecardscan.payment.ml.ssd.rearrangeOCRArray
import java.nio.ByteBuffer

/**
 * We use the output from last two layers with feature maps 19x19 and 10x10
 * and for each feature map activation we have 6 priors, so total priors are
 * 19x19x6 + 10x10x6 = 2766
 */
private const val NUM_OF_PRIORS = 3420

/**
 * For each activation in our feature map, we have predictions for 6 bounding boxes
 * of different aspect ratios
 */
private const val NUM_OF_PRIORS_PER_ACTIVATION = 3

/**
 * We can detect a total of 10 numbers (0 - 9) plus the background class
 */
private const val NUM_OF_CLASSES = 11

/**
 * Each prior or bounding box can be represented by 4 coordinates
 * XMin, YMin, XMax, YMax.
 */
private const val NUM_OF_COORDINATES = 4

/**
 * Represents the total number of data points for locations
 */
private const val NUM_LOC = NUM_OF_COORDINATES * NUM_OF_PRIORS

/**
 * Represents the total number of data points for classes
 */
private const val NUM_CLASS = NUM_OF_CLASSES * NUM_OF_PRIORS

private const val PROB_THRESHOLD = 0.50f
private const val IOU_THRESHOLD = 0.50f
private const val CENTER_VARIANCE = 0.1f
private const val SIZE_VARIANCE = 0.2f
private const val LIMIT = 20

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal const val VERTICAL_THRESHOLD = 2.0f

private val FEATURE_MAP_SIZES =
    OcrFeatureMapSizes(
        layerOneWidth = 38,
        layerOneHeight = 24,
        layerTwoWidth = 19,
        layerTwoHeight = 12
    )

/**
 * This value should never change, and is thread safe.
 */
private val PRIORS = combinePriors(SSDOcr.Factory.TRAINED_IMAGE_SIZE)

/**
 * This model performs SSD OCR recognition on a card.
 */
internal class SSDOcr private constructor(interpreter: InterpreterWrapper) :
    TensorFlowLiteAnalyzer<
        CardOcr.Input,
        Array<ByteBuffer>,
        CardOcr.Prediction,
        Map<Int, Array<FloatArray>>
        >(interpreter) {

    override suspend fun transformData(data: CardOcr.Input): Array<ByteBuffer> =
        arrayOf(data.ssdOcrImage.getData())

    override suspend fun interpretMLOutput(
        data: CardOcr.Input,
        mlOutput: Map<Int, Array<FloatArray>>
    ): CardOcr.Prediction {
        val outputClasses = mlOutput[0] ?: arrayOf(FloatArray(NUM_CLASS))
        val outputLocations = mlOutput[1] ?: arrayOf(FloatArray(NUM_LOC))

        val boxes = rearrangeOCRArray(
            locations = outputLocations,
            featureMapSizes = FEATURE_MAP_SIZES,
            numberOfPriors = NUM_OF_PRIORS_PER_ACTIVATION,
            locationsPerPrior = NUM_OF_COORDINATES
        ).reshape(NUM_OF_COORDINATES)
        boxes.adjustLocations(
            priors = PRIORS,
            centerVariance = CENTER_VARIANCE,
            sizeVariance = SIZE_VARIANCE
        )
        boxes.forEach { it.toRectForm() }

        val scores = rearrangeOCRArray(
            locations = outputClasses,
            featureMapSizes = FEATURE_MAP_SIZES,
            numberOfPriors = NUM_OF_PRIORS_PER_ACTIVATION,
            locationsPerPrior = NUM_OF_CLASSES
        ).reshape(NUM_OF_CLASSES)
        scores.forEach { it.softMax() }

        val detectedBoxes = determineLayoutAndFilter(
            extractPredictions(
                scores = scores,
                boxes = boxes,
                probabilityThreshold = PROB_THRESHOLD,
                intersectionOverUnionThreshold = IOU_THRESHOLD,
                limit = LIMIT,
                classifierToLabel = { if (it == 10) 0 else it }
            ),
            VERTICAL_THRESHOLD
        )

        val predictedNumber = detectedBoxes.map { it.label }.joinToString("")
        return if (isValidPan(predictedNumber)) {
            CardOcr.Prediction(predictedNumber)
        } else {
            CardOcr.Prediction(null)
        }
    }

    override suspend fun executeInference(
        tfInterpreter: InterpreterWrapper,
        data: Array<ByteBuffer>
    ): Map<Int, Array<FloatArray>> {
        val mlOutput = mapOf(
            0 to arrayOf(FloatArray(NUM_CLASS)),
            1 to arrayOf(FloatArray(NUM_LOC))
        )

        @Suppress("UNCHECKED_CAST")
        tfInterpreter.runForMultipleInputsOutputs(data as Array<Any>, mlOutput)
        return mlOutput
    }

    /**
     * A factory for creating instances of this analyzer.
     */
    class Factory(
        context: Context,
        fetchedModel: FetchedData,
        threads: Int = DEFAULT_THREADS
    ) : TFLAnalyzerFactory<CardOcr.Input, CardOcr.Prediction, SSDOcr>(context, fetchedModel) {
        companion object {
            private const val USE_GPU = false
            private const val DEFAULT_THREADS = 4

            val TRAINED_IMAGE_SIZE = Size(600, 375)
        }

        override val tfOptions = InterpreterOptionsWrapper.Builder()
            .useNNAPI(USE_GPU && hasOpenGl31(context.applicationContext))
            .numThreads(threads)
            .build()

        override suspend fun newInstance(): SSDOcr? = createInterpreter()?.let { SSDOcr(it) }
    }
}
